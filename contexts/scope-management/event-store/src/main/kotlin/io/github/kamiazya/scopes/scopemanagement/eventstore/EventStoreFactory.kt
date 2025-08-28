package io.github.kamiazya.scopes.scopemanagement.eventstore

import co.touchlab.kermit.Logger
import io.github.kamiazya.scopes.scopemanagement.eventstore.config.EventStoreConfig
import io.github.kamiazya.scopes.scopemanagement.eventstore.sqlite.SqliteEventStore
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

/**
 * Factory for creating EventStore instances.
 */
object EventStoreFactory {

    /**
     * Creates a SQLite-based EventStore instance.
     */
    fun createSqliteEventStore(config: EventStoreConfig = EventStoreConfig(), logger: Logger = Logger.withTag("EventStore")): EventStore {
        // Note: Directory creation should be handled by the platform-specific implementation
        // or by the caller. SQLite driver will create the database file if it doesn't exist.

        // Create database connection
        val database = Database.connect(
            url = "jdbc:sqlite:${config.databasePath}",
            driver = "org.sqlite.JDBC",
        )

        // SQLite configuration is handled by the driver defaults

        // Create JSON serializer with polymorphic support
        val json = Json {
            prettyPrint = false
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        return SqliteEventStore(
            database = database,
            json = json,
            logger = logger,
        )
    }
}
