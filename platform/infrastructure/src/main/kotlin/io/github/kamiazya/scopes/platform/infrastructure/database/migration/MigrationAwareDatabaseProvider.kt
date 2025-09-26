package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import app.cash.sqldelight.db.SqlDriver
import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.infrastructure.database.ManagedSqlDriver
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.datetime.Clock

/**
 * A generic database provider that automatically handles schema migrations for any SQLDelight database type.
 *
 * This provider wraps the standard SqlDelightDatabaseProvider functionality
 * with automatic migration support. It can validate the schema on startup,
 * apply pending migrations, and ensure database consistency.
 *
 * @param T The type of the SQLDelight database (e.g., ScopeManagementDatabase)
 * @param migrationProvider Function that provides the list of migrations to apply
 * @param config Migration configuration options
 * @param logger Logger for migration progress
 * @param databaseFactory Factory function to create the database instance from a SqlDriver
 */
class MigrationAwareDatabaseProvider<T>(
    private val migrationProvider: () -> List<Migration>,
    private val config: MigrationConfig = MigrationConfig(),
    private val logger: Logger,
    private val databaseFactory: (SqlDriver) -> T,
    private val clock: Clock = Clock.System,
) {

    /**
     * Creates a database with automatic migration support.
     *
     * @param databasePath Path to the SQLite database file
     * @return Either an error or database initialization result
     */
    suspend fun createDatabase(databasePath: String): Either<MigrationError, T> = either {
        logger.info("Initializing migration-aware database at: $databasePath")

        // Create the managed driver with default SQLite settings
        val managedDriver = ManagedSqlDriver.createWithDefaults(databasePath)
        val driver = managedDriver.driver

        // Initialize migration components
        val platformDatabase = io.github.kamiazya.scopes.platform.db.PlatformDatabase(driver)
        val executor = SqlDelightMigrationExecutor(driver)
        val repository = SqlDelightSchemaVersionStore(platformDatabase)
        val migrationManager = DefaultMigrationManager(
            executor = executor,
            repository = repository,
            migrationProvider = migrationProvider,
            clock = clock,
        )

        // Ensure schema_versions table exists
        executor.ensureSchemaVersionsTable().bind()

        // Always apply pending migrations
        logger.debug("Checking for pending migrations")
        val status = migrationManager.getStatus().bind()

        if (!status.isUpToDate) {
            logger.info("Applying ${status.pendingMigrations.size} pending migrations")

            val migrationResult = migrationManager.migrateUp().bind()

            logger.info(
                "Applied ${migrationResult.executedMigrations.size} migrations " +
                    "in ${migrationResult.totalExecutionTime.inWholeMilliseconds}ms " +
                    "(${migrationResult.fromVersion} -> ${migrationResult.toVersion})",
            )
        } else {
            logger.debug("Database is up to date (version ${status.currentVersion})")
        }

        // Create the specific database instance
        val database = databaseFactory(driver)
        logger.info("Database initialized successfully")

        database
    }

    /**
     * Creates an in-memory database for testing with migration support.
     *
     * @return Either an error or the database instance
     */
    suspend fun createInMemoryDatabase(): Either<MigrationError, T> = either {
        logger.debug("Creating in-memory migration-aware database")

        val managedDriver = ManagedSqlDriver(":memory:")
        val driver = managedDriver.driver

        // Initialize migration components
        val platformDatabase = io.github.kamiazya.scopes.platform.db.PlatformDatabase(driver)
        val executor = SqlDelightMigrationExecutor(driver)
        val repository = SqlDelightSchemaVersionStore(platformDatabase)
        val migrationManager = DefaultMigrationManager(
            executor = executor,
            repository = repository,
            migrationProvider = migrationProvider,
            clock = clock,
        )

        // Ensure schema_versions table exists
        executor.ensureSchemaVersionsTable().bind()

        // Always migrate in-memory databases to latest
        logger.debug("Applying all migrations to in-memory database")
        val migrationResult = migrationManager.migrateUp().bind()

        logger.info(
            "Applied ${migrationResult.executedMigrations.size} migrations to in-memory database",
        )

        // Create the specific database instance
        val database = databaseFactory(driver)
        logger.debug("In-memory database initialized successfully")

        database
    }

    /**
     * Creates a database manager for an existing database connection.
     *
     * @param driver The existing SQL driver
     * @return Either an error or migration manager
     */
    fun createMigrationManager(driver: SqlDriver): Either<MigrationError, DefaultMigrationManager> = either {
        val platformDatabase = io.github.kamiazya.scopes.platform.db.PlatformDatabase(driver)
        val executor = SqlDelightMigrationExecutor(driver)
        val repository = SqlDelightSchemaVersionStore(platformDatabase)

        DefaultMigrationManager(
            executor = executor,
            repository = repository,
            migrationProvider = migrationProvider,
            clock = clock,
        )
    }
}
