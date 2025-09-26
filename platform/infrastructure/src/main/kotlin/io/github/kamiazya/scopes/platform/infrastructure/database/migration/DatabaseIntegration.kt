package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import app.cash.sqldelight.db.SqlDriver
import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.db.PlatformDatabase
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Integration utilities for adding migration support to existing database providers.
 *
 * This provides extension functions and utilities to add migration capabilities
 * to existing SQLDelight database providers without breaking existing code.
 */
object DatabaseIntegration {

    /**
     * Applies migrations to an existing SQLite driver before database creation.
     *
     * @param driver The SQL driver to apply migrations to
     * @param migrations List of available migrations
     * @param logger Logger for migration progress
     * @return Either an error or migration result
     */
    suspend fun applyMigrations(driver: SqlDriver, migrations: List<Migration>, logger: Logger): Either<MigrationError, MigrationSummary> = either {
        logger.debug("Applying migrations to database")

        val platformDatabase = PlatformDatabase(driver)
        val executor = SqlDelightMigrationExecutor(driver)
        val repository = SqlDelightSchemaVersionStore(platformDatabase)
        val migrationManager = DefaultMigrationManager(
            executor = executor,
            repository = repository,
            migrationProvider = { migrations },
        )

        // Ensure schema_versions table exists
        executor.ensureSchemaVersionsTable().bind()

        // Apply all pending migrations
        val result = migrationManager.migrateUp().bind()

        if (result.executedMigrations.isNotEmpty()) {
            logger.info(
                "Applied ${result.executedMigrations.size} migrations " +
                    "in ${result.totalExecutionTime}ms",
            )
        } else {
            logger.debug("Database is already up to date")
        }

        result
    }

    /**
     * Validates the migration state of an existing database.
     *
     * @param driver The SQL driver to validate
     * @param migrations List of available migrations
     * @param logger Logger for validation messages
     * @return Either an error or validation result
     */
    suspend fun validateMigrations(driver: SqlDriver, migrations: List<Migration>, logger: Logger): Either<MigrationError, SequenceValidationReport> = either {
        logger.debug("Validating migration state")

        val platformDatabase = PlatformDatabase(driver)
        val executor = SqlDelightMigrationExecutor(driver)
        val repository = SqlDelightSchemaVersionStore(platformDatabase)
        val migrationManager = DefaultMigrationManager(
            executor = executor,
            repository = repository,
            migrationProvider = { migrations },
        )

        // Ensure schema_versions table exists first
        executor.ensureSchemaVersionsTable().bind()

        val result = migrationManager.validate(repair = false).bind()

        if (!result.isValid) {
            logger.warn("Migration validation failed: ${result.inconsistencies}")
        } else {
            logger.debug("Migration state is valid")
        }

        result
    }

    /**
     * Gets the current migration status of a database.
     *
     * @param driver The SQL driver to check
     * @param migrations List of available migrations
     * @return Either an error or migration status
     */
    suspend fun getMigrationStatus(driver: SqlDriver, migrations: List<Migration>): Either<MigrationError, MigrationStatusReport> = either {
        val platformDatabase = PlatformDatabase(driver)
        val executor = SqlDelightMigrationExecutor(driver)
        val repository = SqlDelightSchemaVersionStore(platformDatabase)
        val migrationManager = DefaultMigrationManager(
            executor = executor,
            repository = repository,
            migrationProvider = { migrations },
        )

        // Ensure schema_versions table exists first
        executor.ensureSchemaVersionsTable().bind()

        migrationManager.getStatus().bind()
    }

    /**
     * Creates a migration manager for an existing database driver.
     *
     * @param driver The SQL driver to create manager for
     * @param migrations List of available migrations
     * @return Either an error or migration manager
     */
    suspend fun createMigrationManager(driver: SqlDriver, migrations: List<Migration>): Either<MigrationError, DefaultMigrationManager> = either {
        val platformDatabase = PlatformDatabase(driver)
        val executor = SqlDelightMigrationExecutor(driver)
        val repository = SqlDelightSchemaVersionStore(platformDatabase)

        // Ensure schema_versions table exists first
        executor.ensureSchemaVersionsTable().bind()

        DefaultMigrationManager(
            executor = executor,
            repository = repository,
            migrationProvider = { migrations },
        )
    }
}

/**
 * Extension functions for SqlDriver to add migration capabilities.
 */

/**
 * Apply migrations to this SQL driver.
 */
suspend fun SqlDriver.applyMigrations(migrations: List<Migration>, logger: Logger): Either<MigrationError, MigrationSummary> =
    DatabaseIntegration.applyMigrations(this, migrations, logger)

/**
 * Validate migrations on this SQL driver.
 */
suspend fun SqlDriver.validateMigrations(migrations: List<Migration>, logger: Logger): Either<MigrationError, SequenceValidationReport> =
    DatabaseIntegration.validateMigrations(this, migrations, logger)

/**
 * Get migration status for this SQL driver.
 */
suspend fun SqlDriver.getMigrationStatus(migrations: List<Migration>): Either<MigrationError, MigrationStatusReport> =
    DatabaseIntegration.getMigrationStatus(this, migrations)

/**
 * Create a migration manager for this SQL driver.
 */
suspend fun SqlDriver.createMigrationManager(migrations: List<Migration>): Either<MigrationError, DefaultMigrationManager> =
    DatabaseIntegration.createMigrationManager(this, migrations)
