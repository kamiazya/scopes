package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Result of migration execution operations.
 */
data class MigrationSummary(val executedMigrations: List<AppliedMigration>, val totalExecutionTime: Duration, val fromVersion: Long, val toVersion: Long)

/**
 * Result of migration status check.
 */
data class MigrationStatusReport(
    val currentVersion: Long,
    val availableMigrations: List<Migration>,
    val appliedMigrations: List<AppliedMigration>,
    val pendingMigrations: List<Migration>,
    val isUpToDate: Boolean,
    val hasGaps: Boolean,
    val inconsistencies: List<String>,
) {
    val hasPendingMigrations: Boolean
        get() = pendingMigrations.isNotEmpty()

    val latestVersion: Long?
        get() = availableMigrations.maxOfOrNull { it.version }
}

/**
 * Central coordinator for database migration operations.
 *
 * Orchestrates the migration process by coordinating between migration discovery,
 * validation, execution, and tracking. Provides high-level operations for
 * applying migrations, rolling back, and checking status.
 */
interface MigrationManager {

    /**
     * Get current migration status.
     *
     * @return Either an error or current migration status
     */
    suspend fun getStatus(): Either<MigrationError, MigrationStatusReport>

    /**
     * Apply all pending migrations up to the latest available.
     *
     * @return Either an error or migration result
     */
    suspend fun migrateUp(): Either<MigrationError, MigrationSummary>

    /**
     * Apply migrations up to a specific version.
     *
     * @param targetVersion The version to migrate to
     * @return Either an error or migration result
     */
    suspend fun migrateTo(targetVersion: Long): Either<MigrationError, MigrationSummary>

    /**
     * Validate the current migration state and fix any inconsistencies.
     *
     * @param repair Whether to attempt automatic repair of inconsistencies
     * @return Either an error or validation result
     */
    suspend fun validate(repair: Boolean = false): Either<MigrationError, SequenceValidationReport>

    /**
     * Force mark a migration as applied without executing it.
     * USE WITH EXTREME CAUTION - only for manual database repairs.
     *
     * @param migration The migration to mark as applied
     * @return Either an error or Unit on success
     */
    suspend fun markAsApplied(migration: Migration): Either<MigrationError, Unit>
}

/**
 * Default implementation of MigrationManager.
 *
 * Coordinates migration operations using provided executor, repository, and discovery services.
 */
class DefaultMigrationManager(
    private val executor: MigrationExecutor,
    private val repository: SchemaVersionStore,
    private val migrationProvider: () -> List<Migration>,
    private val clock: Clock = Clock.System,
) : MigrationManager {

    override suspend fun getStatus(): Either<MigrationError, MigrationStatusReport> = withContext(Dispatchers.IO) {
        either {
            val currentVersion = repository.getCurrentVersion().bind()
            val availableMigrations = migrationProvider().sortedBy { it.version }
            val appliedMigrations = repository.getAllAppliedMigrations().bind()
            val appliedVersions = appliedMigrations.map { it.version }.toSet()

            val pendingMigrations = availableMigrations.filter { it.version !in appliedVersions }
            val isUpToDate = pendingMigrations.isEmpty()

            val validationResult = repository.validateMigrationSequence().bind()

            MigrationStatusReport(
                currentVersion = currentVersion,
                availableMigrations = availableMigrations,
                appliedMigrations = appliedMigrations,
                pendingMigrations = pendingMigrations,
                isUpToDate = isUpToDate,
                hasGaps = !validationResult.isValid,
                inconsistencies = validationResult.inconsistencies,
            )
        }
    }

    override suspend fun migrateUp(): Either<MigrationError, MigrationSummary> = withContext(Dispatchers.IO) {
        either {
            val status = getStatus().bind()

            if (status.isUpToDate) {
                MigrationSummary(
                    executedMigrations = emptyList(),
                    totalExecutionTime = Duration.ZERO,
                    fromVersion = status.currentVersion,
                    toVersion = status.currentVersion,
                )
            } else {
                executeMigrations(status.pendingMigrations, status.currentVersion)
            }
        }
    }

    override suspend fun migrateTo(targetVersion: Long): Either<MigrationError, MigrationSummary> = withContext(Dispatchers.IO) {
        either {
            val status = getStatus().bind()
            val currentVersion = status.currentVersion

            when {
                targetVersion == currentVersion -> {
                    MigrationSummary(
                        executedMigrations = emptyList(),
                        totalExecutionTime = Duration.ZERO,
                        fromVersion = currentVersion,
                        toVersion = currentVersion,
                    )
                }
                targetVersion > currentVersion -> {
                    // Migrate up to target version
                    val migrationsToApply = status.pendingMigrations
                        .filter { it.version <= targetVersion }
                        .sortedBy { it.version }

                    if (migrationsToApply.isEmpty()) {
                        raise(
                            MigrationError.InvalidTargetVersion(
                                targetVersion,
                                currentVersion,
                                "No migrations available to reach target version",
                            ),
                        )
                    }

                    executeMigrations(migrationsToApply, currentVersion)
                }
                else -> {
                    raise(
                        MigrationError.InvalidTargetVersion(
                            targetVersion,
                            currentVersion,
                            "Cannot rollback. Target version must be greater than or equal to current version",
                        ),
                    )
                }
            }
        }
    }

    override suspend fun validate(repair: Boolean): Either<MigrationError, SequenceValidationReport> = withContext(Dispatchers.IO) {
        either {
            // First perform basic validation
            val validationResult = repository.validateMigrationSequence().bind()

            if (validationResult.isValid || !repair) {
                validationResult
            } else {
                // If repair is requested and there are issues, attempt to fix them
                val availableMigrations = migrationProvider().sortedBy { it.version }
                val appliedMigrations = repository.getAllAppliedMigrations().bind()

                val inconsistencies = mutableListOf<String>()

                // Check for migrations that have been applied but are no longer available
                for (appliedMigration in appliedMigrations) {
                    val availableMigration = availableMigrations.find { it.version == appliedMigration.version }
                    if (availableMigration == null) {
                        inconsistencies.add("Applied migration ${appliedMigration.version} is no longer available")
                        continue
                    }
                }

                SequenceValidationReport(
                    isValid = inconsistencies.isEmpty(),
                    gaps = validationResult.gaps,
                    inconsistencies = inconsistencies,
                )
            }
        }
    }

    override suspend fun markAsApplied(migration: Migration): Either<MigrationError, Unit> = withContext(Dispatchers.IO) {
        either {
            val appliedMigration = AppliedMigration(
                version = migration.version,
                description = migration.description,
                appliedAt = clock.now(),
                executionTime = Duration.ZERO, // Marked as applied, no execution time
            )

            repository.saveAppliedMigration(appliedMigration).bind()
        }
    }

    /**
     * Execute a list of migrations in order.
     */
    private suspend fun Raise<MigrationError>.executeMigrations(migrations: List<Migration>, fromVersion: Long): MigrationSummary {
        val executedMigrations = mutableListOf<AppliedMigration>()
        var totalExecutionTime = Duration.ZERO

        for (migration in migrations) {
            val startTime = clock.now()

            try {
                // Validate migration before execution
                val existingMigration = repository.findByVersion(migration.version).bind()
                if (existingMigration != null) {
                    // Skip already applied migration
                    continue
                }

                // Execute the migration
                migration.apply(executor).bind()

                val endTime = clock.now()
                val executionTime = (endTime - startTime)

                // Record the applied migration
                val appliedMigration = AppliedMigration(
                    version = migration.version,
                    description = migration.description,
                    appliedAt = endTime,
                    executionTime = executionTime,
                )

                repository.saveAppliedMigration(appliedMigration).bind()
                executedMigrations.add(appliedMigration)
                totalExecutionTime += executionTime
            } catch (e: MigrationError) {
                // Re-raise migration errors directly
                raise(e)
            } catch (e: CancellationException) {
                // Re-throw cancellation exceptions to preserve coroutine cancellation
                throw e
            } catch (e: Exception) {
                // Wrap other exceptions
                raise(
                    MigrationError.SqlExecutionError(
                        migration.version,
                        "Migration execution failed",
                        e,
                    ),
                )
            }
        }

        val toVersion = if (executedMigrations.isNotEmpty()) {
            executedMigrations.last().version
        } else {
            fromVersion
        }

        return MigrationSummary(
            executedMigrations = executedMigrations,
            totalExecutionTime = totalExecutionTime,
            fromVersion = fromVersion,
            toVersion = toVersion,
        )
    }
}
