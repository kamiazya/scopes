package io.github.kamiazya.scopes.platform.infrastructure.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger

/**
 * Manages database migrations for SQLDelight databases.
 *
 * This manager provides:
 * - Automatic migration execution based on version differences
 * - Safe migration with transaction support
 * - Logging of migration progress
 * - Version tracking in user_version PRAGMA
 * - Thread-safe migration execution
 */
class DatabaseMigrationManager(
    private val logger: Logger = ConsoleLogger()
) {
    /**
     * Executes database migration if needed.
     * This method is synchronized to prevent concurrent migrations.
     *
     * @param driver The SQL driver to execute migrations on
     * @param schema The SQLDelight schema containing migration logic
     * @param targetVersion The target schema version to migrate to
     * @param callbacks Optional callbacks for custom migration logic at specific versions
     * @throws IllegalStateException if migration fails or database is newer than application
     */
    @Synchronized
    fun migrate(
        driver: SqlDriver,
        schema: SqlSchema<*>,
        targetVersion: Long,
        callbacks: Map<Long, MigrationCallback> = emptyMap()
    ) {
        // Use database-level locking to prevent concurrent migrations across processes
        var transactionActive = false
        try {
            driver.execute(null, "BEGIN IMMEDIATE", 0)
            transactionActive = true
            performMigrationInternal(driver, schema, targetVersion, callbacks)
            driver.execute(null, "COMMIT", 0)
            transactionActive = false
        } catch (e: Exception) {
            if (transactionActive) {
                try {
                    driver.execute(null, "ROLLBACK", 0)
                } catch (rollbackException: Exception) {
                    // Log rollback failure but preserve original exception
                    logger.error("Failed to rollback migration transaction", throwable = rollbackException)
                }
            }
            error("Migration failed: ${e.message}")
        }
    }

    /**
     * Internal migration logic, called within database transaction lock.
     */
    private fun performMigrationInternal(
        driver: SqlDriver,
        schema: SqlSchema<*>,
        targetVersion: Long,
        callbacks: Map<Long, MigrationCallback>
    ) {
        val currentVersion = getCurrentVersion(driver)

        when {
            currentVersion == 0L && !isDatabaseEmpty(driver) -> {
                // Database exists but no version set - likely pre-migration database
                logger.warn("Database exists without version. Setting version to 1 and attempting migration.")
                setVersion(driver, 1L)
                if (targetVersion > 1L) {
                    executeSchemaUpdate(driver, schema, 1L, targetVersion, callbacks)
                }
            }
            currentVersion == 0L -> {
                // Fresh database
                logger.info("Creating new database schema (version $targetVersion)")
                try {
                    schema.create(driver)
                    setVersion(driver, targetVersion)
                } catch (e: Exception) {
                    error("Failed to create database schema: ${e.message}")
                }
            }
            currentVersion < targetVersion -> {
                logger.info("Migrating database from version $currentVersion to $targetVersion")
                executeSchemaUpdate(driver, schema, currentVersion, targetVersion, callbacks)
            }
            currentVersion > targetVersion -> {
                // Fail fast when database is newer than application
                val message = "Database version ($currentVersion) is newer than application version ($targetVersion). " +
                        "Please update the application to a newer version."
                logger.error(message)
                error(message)
            }
            else -> {
                logger.debug("Database is up to date (version $currentVersion)")
            }
        }
    }

    /**
     * Checks if the database is empty (no tables).
     */
    private fun isDatabaseEmpty(driver: SqlDriver): Boolean {
        return driver.executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
            mapper = { cursor ->
                QueryResult.Value(
                    if (cursor.next().value) {
                        cursor.getLong(0) == 0L
                    } else {
                        true
                    }
                )
            },
            parameters = 0
        ).value
    }

    /**
     * Gets the current database schema version.
     * SQLite stores this in the user_version PRAGMA.
     */
    private fun getCurrentVersion(driver: SqlDriver): Long {
        return driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                QueryResult.Value(
                    if (cursor.next().value) {
                        cursor.getLong(0) ?: 0L
                    } else {
                        0L
                    }
                )
            },
            parameters = 0
        ).value
    }

    /**
     * Sets the database schema version.
     */
    private fun setVersion(driver: SqlDriver, version: Long) {
        driver.execute(
            identifier = null,
            sql = "PRAGMA user_version = $version",
            parameters = 0
        )
    }

    /**
     * Performs the actual schema update from one version to another.
     */
    private fun executeSchemaUpdate(
        driver: SqlDriver,
        schema: SqlSchema<*>,
        currentVersion: Long,
        targetVersion: Long,
        callbacks: Map<Long, MigrationCallback>
    ) {
        try {
            // Execute SQLDelight migrations
            schema.migrate(driver, currentVersion, targetVersion)

            // Note: The callbacks parameter with afterVersion lambda is not part of the standard migrate API
            // If custom callbacks are needed at specific versions, they should be handled separately
            for (version in (currentVersion + 1)..targetVersion) {
                callbacks[version]?.let { callback ->
                    logger.debug("Executing custom migration callback for version $version")
                    callback.execute(driver)
                }
            }

            // Update version
            setVersion(driver, targetVersion)

            logger.info("Migration completed successfully")
        } catch (e: Exception) {
            logger.error("Migration failed", throwable = e)
            error("Failed to migrate database from $currentVersion to $targetVersion: ${e.message}")
        }
    }

    /**
     * Callback interface for custom migration logic.
     */
    fun interface MigrationCallback {
        /**
         * Execute custom migration logic.
         */
        fun execute(driver: SqlDriver)
    }

    companion object {
        /**
         * Creates a migration manager with default configuration.
         */
        fun createDefault(): DatabaseMigrationManager {
            return DatabaseMigrationManager()
        }
    }
}

/**
 * Exception thrown when database migration fails.
 */
class DatabaseMigrationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)