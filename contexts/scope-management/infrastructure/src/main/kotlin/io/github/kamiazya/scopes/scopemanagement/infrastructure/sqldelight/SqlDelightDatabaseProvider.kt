package io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight

import io.github.kamiazya.scopes.platform.infrastructure.database.DatabaseMigrationManager
import io.github.kamiazya.scopes.platform.infrastructure.database.ManagedSqlDriver
import io.github.kamiazya.scopes.platform.infrastructure.version.ApplicationVersion
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase

/**
 * Provides SQLDelight database instances for Scope Management.
 *
 * This provider creates databases with automatic resource management and migration support.
 * The returned ManagedDatabase wrapper ensures proper cleanup on close.
 */
object SqlDelightDatabaseProvider {

    private val migrationManager = DatabaseMigrationManager.createDefault()

    /**
     * Wrapper that combines database and its managed driver for proper cleanup.
     * Implements AutoCloseable to ensure resources are properly released.
     */
    class ManagedDatabase(
        private val database: ScopeManagementDatabase,
        private val managedDriver: AutoCloseable
    ) : ScopeManagementDatabase by database, AutoCloseable {
        override fun close() {
            // Close the driver to release file handles and WAL locks
            managedDriver.close()
        }
    }

    /**
     * Creates a new ScopeManagementDatabase instance with automatic resource management.
     * Automatically handles schema creation and migration based on version differences.
     *
     * @return ManagedDatabase that must be closed when no longer needed
     */
    fun createDatabase(databasePath: String): ManagedDatabase {
        val managedDriver = ManagedSqlDriver.createWithDefaults(databasePath)
        val driver = managedDriver.driver

        // Perform migration if needed
        migrationManager.migrate(
            driver = driver,
            schema = ScopeManagementDatabase.Schema,
            targetVersion = ApplicationVersion.SchemaVersions.SCOPE_MANAGEMENT
        )

        return ManagedDatabase(ScopeManagementDatabase(driver), managedDriver)
    }

    /**
     * Creates an in-memory database for testing.
     * Always creates a fresh schema without migration.
     *
     * @return ManagedDatabase that must be closed when no longer needed
     */
    fun createInMemoryDatabase(): ManagedDatabase {
        val managedDriver = ManagedSqlDriver(":memory:")
        val driver = managedDriver.driver

        // For in-memory databases, always create fresh schema
        ScopeManagementDatabase.Schema.create(driver)

        // Set the version for consistency
        driver.execute(
            identifier = null,
            sql = "PRAGMA user_version = ${ApplicationVersion.SchemaVersions.SCOPE_MANAGEMENT}",
            parameters = 0
        )

        return ManagedDatabase(ScopeManagementDatabase(driver), managedDriver)
    }
}
