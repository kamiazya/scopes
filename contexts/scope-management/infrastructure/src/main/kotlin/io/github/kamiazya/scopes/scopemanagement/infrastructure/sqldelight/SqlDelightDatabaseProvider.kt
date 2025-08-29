package io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight

import io.github.kamiazya.scopes.platform.infrastructure.database.ManagedSqlDriver
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase

/**
 * Provides SQLDelight database instances for Scope Management.
 *
 * This provider creates databases with automatic resource management.
 * The returned ManagedDatabase wrapper ensures proper cleanup on close.
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

    /**
     * Creates a new ScopeManagementDatabase instance with automatic resource management.
     */
    fun createDatabase(databasePath: String): ScopeManagementDatabase {
        val managedDriver = ManagedSqlDriver.createWithDefaults(databasePath)
        val driver = managedDriver.driver

        // Create the database schema
        ScopeManagementDatabase.Schema.create(driver)

        return ManagedDatabase(ScopeManagementDatabase(driver), managedDriver)
    }

    /**
     * Creates an in-memory database for testing.
     */
    fun createInMemoryDatabase(): ScopeManagementDatabase {
        val managedDriver = ManagedSqlDriver(":memory:")
        val driver = managedDriver.driver

        ScopeManagementDatabase.Schema.create(driver)
        return ManagedDatabase(ScopeManagementDatabase(driver), managedDriver)
    }
}
