package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.commons.time.Instant
import io.github.kamiazya.scopes.platform.db.PlatformDatabase
import io.github.kamiazya.scopes.platform.db.Schema_versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Repository interface for managing applied migration records.
 */
interface SchemaVersionStore {

    /**
     * Save a record of an applied migration.
     *
     * @param migration The applied migration record to save
     * @return Either an error or Unit on success
     */
    suspend fun saveAppliedMigration(migration: AppliedMigration): Either<MigrationError, Unit>

    /**
     * Get all applied migrations ordered by version.
     *
     * @return Either an error or list of applied migrations
     */
    suspend fun getAllAppliedMigrations(): Either<MigrationError, List<AppliedMigration>>

    /**
     * Get applied migrations with pagination.
     *
     * @param limit Maximum number of migrations to return
     * @param offset Number of migrations to skip
     * @return Either an error or list of applied migrations
     */
    suspend fun getAppliedMigrations(limit: Int, offset: Int): Either<MigrationError, List<AppliedMigration>>

    /**
     * Find a specific applied migration by version.
     *
     * @param version The migration version to find
     * @return Either an error or the applied migration (null if not found)
     */
    suspend fun findByVersion(version: Long): Either<MigrationError, AppliedMigration?>

    /**
     * Get the current (highest) migration version.
     * Returns 0 if no migrations have been applied.
     *
     * @return Either an error or the current version
     */
    suspend fun getCurrentVersion(): Either<MigrationError, Long>

    /**
     * Check if a specific migration version has been applied.
     *
     * @param version The migration version to check
     * @return Either an error or true if applied, false otherwise
     */
    suspend fun isVersionApplied(version: Long): Either<MigrationError, Boolean>

    /**
     * Get migration statistics.
     *
     * @return Either an error or migration statistics
     */
    suspend fun getMigrationStatistics(): Either<MigrationError, MigrationStatistics>

    /**
     * Validate the migration sequence for gaps or inconsistencies.
     *
     * @return Either an error or validation result
     */
    suspend fun validateMigrationSequence(): Either<MigrationError, SequenceValidationReport>
}

/**
 * Statistics about applied migrations.
 */
data class MigrationStatistics(
    val totalMigrations: Long,
    val firstVersion: Long?,
    val currentVersion: Long?,
    val firstApplied: Instant?,
    val lastApplied: Instant?,
    val totalExecutionTime: Duration,
)

/**
 * Result of migration sequence validation.
 */
data class SequenceValidationReport(val isValid: Boolean, val gaps: List<Long> = emptyList(), val inconsistencies: List<String> = emptyList())

/**
 * SQLDelight implementation of SchemaVersionRepository.
 */
class SqlDelightSchemaVersionStore(private val database: PlatformDatabase) : SchemaVersionStore {

    override suspend fun saveAppliedMigration(migration: AppliedMigration): Either<MigrationError, Unit> = withContext(Dispatchers.IO) {
        try {
            database.schemaVersionQueries.insertMigration(
                version = migration.version,
                description = migration.description,
                applied_at = migration.appliedAt.toEpochMilliseconds(),
                execution_time_ms = migration.executionTime.inWholeMilliseconds,
            )
            Unit.right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "save applied migration",
                cause = e,
            ).left()
        }
    }

    override suspend fun getAllAppliedMigrations(): Either<MigrationError, List<AppliedMigration>> = withContext(Dispatchers.IO) {
        try {
            val migrations = database.schemaVersionQueries.getAllApplied()
                .executeAsList()
                .map { it.toAppliedMigration() }
            migrations.right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "get all applied migrations",
                cause = e,
            ).left()
        }
    }

    override suspend fun getAppliedMigrations(limit: Int, offset: Int): Either<MigrationError, List<AppliedMigration>> = withContext(Dispatchers.IO) {
        try {
            val migrations = database.schemaVersionQueries.getAllAppliedPaged(
                limit.toLong(),
                offset.toLong(),
            ).executeAsList().map { it.toAppliedMigration() }
            migrations.right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "get applied migrations with pagination",
                cause = e,
            ).left()
        }
    }

    override suspend fun findByVersion(version: Long): Either<MigrationError, AppliedMigration?> = withContext(Dispatchers.IO) {
        try {
            val migration = database.schemaVersionQueries.findByVersion(version)
                .executeAsOneOrNull()
                ?.toAppliedMigration()
            migration.right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "find migration by version",
                cause = e,
            ).left()
        }
    }

    override suspend fun getCurrentVersion(): Either<MigrationError, Long> = withContext(Dispatchers.IO) {
        try {
            val version = database.schemaVersionQueries.getCurrentVersion()
                .executeAsOne()
            version.right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "get current version",
                cause = e,
            ).left()
        }
    }

    override suspend fun isVersionApplied(version: Long): Either<MigrationError, Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = database.schemaVersionQueries.existsByVersion(version)
                .executeAsOne()
            exists.right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "check if version is applied",
                cause = e,
            ).left()
        }
    }

    override suspend fun getMigrationStatistics(): Either<MigrationError, MigrationStatistics> = withContext(Dispatchers.IO) {
        try {
            val stats = database.schemaVersionQueries.getMigrationStats().executeAsOne()
            val statistics = MigrationStatistics(
                totalMigrations = stats.count ?: 0L,
                firstVersion = stats.min_version,
                currentVersion = stats.max_version,
                firstApplied = stats.min_applied_at?.let { Instant.fromEpochMilliseconds(it) },
                lastApplied = stats.max_applied_at?.let { Instant.fromEpochMilliseconds(it) },
                totalExecutionTime = (stats.total_execution_time ?: 0L).milliseconds,
            )
            statistics.right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "get migration statistics",
                cause = e,
            ).left()
        }
    }

    override suspend fun validateMigrationSequence(): Either<MigrationError, SequenceValidationReport> = withContext(Dispatchers.IO) {
        try {
            val sequences = database.schemaVersionQueries.validateMigrationSequence()
                .executeAsList()

            val gaps = mutableListOf<Long>()
            val inconsistencies = mutableListOf<String>()

            for (sequence in sequences) {
                val version = sequence.version
                val gap = sequence.gap
                if ((gap ?: 0L) > 1) {
                    gaps.add(version ?: 0L)
                    inconsistencies.add(
                        "Gap detected: missing versions between ${(version ?: 0L) - (gap ?: 0L)} and ${version ?: 0L}",
                    )
                }
            }

            SequenceValidationReport(
                isValid = gaps.isEmpty(),
                gaps = gaps,
                inconsistencies = inconsistencies,
            ).right()
        } catch (e: Exception) {
            MigrationError.DatabaseError(
                operation = "validate migration sequence",
                cause = e,
            ).left()
        }
    }

    private fun Schema_versions.toAppliedMigration(): AppliedMigration = AppliedMigration(
        version = version,
        description = description,
        appliedAt = Instant.fromEpochMilliseconds(applied_at),
        executionTime = execution_time_ms.milliseconds,
    )
}
