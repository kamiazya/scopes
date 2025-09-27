package io.github.kamiazya.scopes.devicesync.infrastructure.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kamiazya.scopes.devicesync.db.DeviceSyncDatabase
import io.github.kamiazya.scopes.platform.infrastructure.database.DatabaseMigrationManager
import io.github.kamiazya.scopes.platform.infrastructure.version.ApplicationVersion

/**
 * Provides SQLDelight database instances for Device Synchronization.
 *
 * This provider creates databases with migration support.
 */
object SqlDelightDatabaseProvider {

    /**
     * Creates a new DeviceSyncDatabase instance.
     * Automatically handles schema creation and migration based on version differences.
     */
    fun createDatabase(databasePath: String): DeviceSyncDatabase {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

        // Perform migration if needed
        val migrationManager = DatabaseMigrationManager.createDefault()
        migrationManager.migrate(
            driver = driver,
            schema = DeviceSyncDatabase.Schema,
            targetVersion = ApplicationVersion.SchemaVersions.DEVICE_SYNCHRONIZATION
        )

        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        return DeviceSyncDatabase(driver)
    }

    /**
     * Creates an in-memory database for testing.
     * Always creates a fresh schema without migration.
     */
    fun createInMemoryDatabase(): DeviceSyncDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // For in-memory databases, always create fresh schema
        DeviceSyncDatabase.Schema.create(driver)

        // Set the version for consistency
        driver.execute(
            identifier = null,
            sql = "PRAGMA user_version = ${ApplicationVersion.SchemaVersions.DEVICE_SYNCHRONIZATION}",
            parameters = 0
        )

        return DeviceSyncDatabase(driver)
    }
}
