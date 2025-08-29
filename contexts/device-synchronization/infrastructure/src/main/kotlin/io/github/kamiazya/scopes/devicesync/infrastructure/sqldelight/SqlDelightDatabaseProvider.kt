package io.github.kamiazya.scopes.devicesync.infrastructure.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kamiazya.scopes.devicesync.db.DeviceSyncDatabase

/**
 * Provides SQLDelight database instances for Device Synchronization.
 */
object SqlDelightDatabaseProvider {

    /**
     * Creates a new DeviceSyncDatabase instance.
     */
    fun createDatabase(databasePath: String): DeviceSyncDatabase {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

        // Create the database schema
        DeviceSyncDatabase.Schema.create(driver)

        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        return DeviceSyncDatabase(driver)
    }

    /**
     * Creates an in-memory database for testing.
     */
    fun createInMemoryDatabase(): DeviceSyncDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        DeviceSyncDatabase.Schema.create(driver)
        return DeviceSyncDatabase(driver)
    }
}
