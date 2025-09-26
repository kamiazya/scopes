package io.github.kamiazya.scopes.eventstore.infrastructure.sqldelight

import io.github.kamiazya.scopes.eventstore.db.EventStoreDatabase
import io.github.kamiazya.scopes.eventstore.infrastructure.migration.EventStoreMigrationProvider
import io.github.kamiazya.scopes.platform.infrastructure.database.ManagedSqlDriver
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.applyMigrations
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import kotlinx.coroutines.runBlocking

/**
 * Provides SQLDelight database instances for Event Store with automatic migrations.
 */
object SqlDelightDatabaseProvider {

    class ManagedDatabase(private val database: EventStoreDatabase, private val managedDriver: AutoCloseable) :
        EventStoreDatabase by database,
        AutoCloseable {
        override fun close() = managedDriver.close()
    }

    fun createDatabase(databasePath: String): EventStoreDatabase {
        val managedDriver = ManagedSqlDriver.createWithDefaults(databasePath)
        val driver = managedDriver.driver

        val logger = ConsoleLogger("EventStoreDB")
        val migrations = EventStoreMigrationProvider(logger).getMigrations()
        runBlocking {
            driver.applyMigrations(migrations, logger).fold(
                ifLeft = { err -> error("Migration failed: ${err.message}") },
                ifRight = { },
            )
        }
        return ManagedDatabase(EventStoreDatabase(driver), managedDriver)
    }

    fun createInMemoryDatabase(): EventStoreDatabase {
        val managedDriver = ManagedSqlDriver(":memory:")
        val driver = managedDriver.driver

        val logger = ConsoleLogger("EventStoreDB-InMemory")
        val migrations = EventStoreMigrationProvider(logger).getMigrations()
        runBlocking {
            driver.applyMigrations(migrations, logger).fold(
                ifLeft = { err -> error("Migration failed: ${err.message}") },
                ifRight = { },
            )
        }
        return ManagedDatabase(EventStoreDatabase(driver), managedDriver)
    }
}
