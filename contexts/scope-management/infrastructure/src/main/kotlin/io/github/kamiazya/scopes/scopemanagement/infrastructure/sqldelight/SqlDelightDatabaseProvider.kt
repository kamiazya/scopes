package io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase

/**
 * Provides SQLDelight database instances for Scope Management.
 */
object SqlDelightDatabaseProvider {

    /**
     * Creates a new ScopeManagementDatabase instance.
     */
    fun createDatabase(databasePath: String): ScopeManagementDatabase {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

        // Create the database schema
        ScopeManagementDatabase.Schema.create(driver)

        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        return ScopeManagementDatabase(driver)
    }

    /**
     * Creates an in-memory database for testing.
     */
    fun createInMemoryDatabase(): ScopeManagementDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ScopeManagementDatabase.Schema.create(driver)
        return ScopeManagementDatabase(driver)
    }
}
