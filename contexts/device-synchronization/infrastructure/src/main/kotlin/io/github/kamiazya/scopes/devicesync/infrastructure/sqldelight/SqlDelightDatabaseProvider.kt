package io.github.kamiazya.scopes.devicesync.infrastructure.sqldelight

import io.github.kamiazya.scopes.devicesync.db.DeviceSyncDatabase
import io.github.kamiazya.scopes.devicesync.infrastructure.migration.DeviceSyncMigrationProvider
import io.github.kamiazya.scopes.platform.infrastructure.database.ManagedSqlDriver
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.applyMigrations
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import kotlinx.coroutines.runBlocking

/**
 * Provides SQLDelight database instances for Device Synchronization with automatic migrations.
 */
object SqlDelightDatabaseProvider {

    class ManagedDatabase(private val database: DeviceSyncDatabase, private val managedDriver: AutoCloseable) :
        DeviceSyncDatabase by database,
        AutoCloseable {
        override fun close() = managedDriver.close()
    }

    fun createDatabase(databasePath: String): DeviceSyncDatabase {
        val managedDriver = ManagedSqlDriver.createWithDefaults(databasePath)
        val driver = managedDriver.driver

        val logger = ConsoleLogger("DeviceSyncDB")
        val migrations = DeviceSyncMigrationProvider(logger).getMigrations()
        runBlocking {
            driver.applyMigrations(migrations, logger).fold(
                ifLeft = { err -> error("Migration failed: ${err.message}") },
                ifRight = { },
            )
        }
        return ManagedDatabase(DeviceSyncDatabase(driver), managedDriver)
    }

    fun createInMemoryDatabase(): DeviceSyncDatabase {
        val managedDriver = ManagedSqlDriver(":memory:")
        val driver = managedDriver.driver

        val logger = ConsoleLogger("DeviceSyncDB-InMemory")
        val migrations = DeviceSyncMigrationProvider(logger).getMigrations()
        runBlocking {
            driver.applyMigrations(migrations, logger).fold(
                ifLeft = { err -> error("Migration failed: ${err.message}") },
                ifRight = { },
            )
        }
        return ManagedDatabase(DeviceSyncDatabase(driver), managedDriver)
    }
}
