package io.github.kamiazya.scopes.eventstore.infrastructure.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kamiazya.scopes.eventstore.db.EventStoreDatabase
import io.github.kamiazya.scopes.platform.infrastructure.database.DatabaseMigrationManager
import io.github.kamiazya.scopes.platform.infrastructure.version.ApplicationVersion

/**
 * Provides SQLDelight database instances for Event Store.
 *
 * This provider creates databases with migration support.
 */
object SqlDelightDatabaseProvider {

    private val migrationManager = DatabaseMigrationManager.createDefault()

    /**
     * Creates a new EventStoreDatabase instance.
     * Automatically handles schema creation and migration based on version differences.
     */
    fun createDatabase(databasePath: String): EventStoreDatabase {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

        // Perform migration if needed
        migrationManager.migrate(
            driver = driver,
            schema = EventStoreDatabase.Schema,
            targetVersion = ApplicationVersion.SchemaVersions.EVENT_STORE
        )

        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        return EventStoreDatabase(driver)
    }

    /**
     * Creates an in-memory database for testing.
     * Always creates a fresh schema without migration.
     */
    fun createInMemoryDatabase(): EventStoreDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // For in-memory databases, always create fresh schema
        EventStoreDatabase.Schema.create(driver)

        // Set the version for consistency
        driver.execute(
            identifier = null,
            sql = "PRAGMA user_version = ${ApplicationVersion.SchemaVersions.EVENT_STORE}",
            parameters = 0
        )

        return EventStoreDatabase(driver)
    }
}
