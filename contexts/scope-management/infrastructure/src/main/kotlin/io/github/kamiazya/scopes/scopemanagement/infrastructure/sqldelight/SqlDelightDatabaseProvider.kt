package io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight

import io.github.kamiazya.scopes.platform.infrastructure.database.ManagedSqlDriver
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.MigrationAwareDatabaseProvider
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.MigrationConfig
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.applyMigrations
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.infrastructure.migration.ScopeManagementMigrationProvider
import kotlinx.coroutines.runBlocking

/**
 * Provides SQLDelight database instances for Scope Management.
 *
 * This provider now uses MigrationAwareDatabaseProvider to create and migrate the database,
 * aligning runtime and test initialization with the same migration source of truth.
 */
object SqlDelightDatabaseProvider {

    /**
     * Wrapper that combines database and its managed driver for proper cleanup.
     */
    class ManagedDatabase(private val database: ScopeManagementDatabase, private val managedDriver: AutoCloseable) :
        ScopeManagementDatabase by database,
        AutoCloseable {
        override fun close() {
            managedDriver.close()
        }
    }

    private fun provider(loggerName: String = "ScopeManagementDB"): MigrationAwareDatabaseProvider<ScopeManagementDatabase> {
        val logger = ConsoleLogger(loggerName)
        val migrations = { ScopeManagementMigrationProvider(logger = logger).getMigrations() }
        return MigrationAwareDatabaseProvider(
            migrationProvider = migrations,
            config = MigrationConfig(maxRetries = 3),
            logger = logger,
            databaseFactory = { driver -> ScopeManagementDatabase(driver) },
        )
    }

    /**
     * Creates a new ScopeManagementDatabase instance with automatic resource management.
     * Applies all pending migrations on the same driver before returning the database.
     */
    fun createDatabase(databasePath: String): ScopeManagementDatabase {
        val managedDriver = ManagedSqlDriver.createWithDefaults(databasePath)
        val driver = managedDriver.driver

        val logger = ConsoleLogger("ScopeManagementDB")
        val migrations = ScopeManagementMigrationProvider(logger = logger).getMigrations()
        runBlocking {
            driver.applyMigrations(migrations, logger).fold(
                ifLeft = { err -> error("Migration failed: ${err.message}") },
                ifRight = { },
            )
        }

        val db = ScopeManagementDatabase(driver)
        return ManagedDatabase(db, managedDriver)
    }

    /**
     * Creates an in-memory database for testing with migrations applied.
     */
    fun createInMemoryDatabase(): ScopeManagementDatabase {
        val managedDriver = ManagedSqlDriver(":memory:")
        val driver = managedDriver.driver

        val logger = ConsoleLogger("ScopeManagementDB-InMemory")
        val migrations = ScopeManagementMigrationProvider(logger = logger).getMigrations()
        runBlocking {
            driver.applyMigrations(migrations, logger).fold(
                ifLeft = { err -> error("Migration failed: ${err.message}") },
                ifRight = { },
            )
        }

        val db = ScopeManagementDatabase(driver)
        return ManagedDatabase(db, managedDriver)
    }
}
