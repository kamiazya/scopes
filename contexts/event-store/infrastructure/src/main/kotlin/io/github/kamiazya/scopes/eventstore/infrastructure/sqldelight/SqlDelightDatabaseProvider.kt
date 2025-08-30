package io.github.kamiazya.scopes.eventstore.infrastructure.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kamiazya.scopes.eventstore.db.EventStoreDatabase

/**
 * Provides SQLDelight database instances for Event Store.
 */
object SqlDelightDatabaseProvider {

    /**
     * Creates a new EventStoreDatabase instance.
     */
    fun createDatabase(databasePath: String): EventStoreDatabase {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

        // Create the database schema
        EventStoreDatabase.Schema.create(driver)

        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        return EventStoreDatabase(driver)
    }

    /**
     * Creates an in-memory database for testing.
     */
    fun createInMemoryDatabase(): EventStoreDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EventStoreDatabase.Schema.create(driver)
        return EventStoreDatabase(driver)
    }
}
