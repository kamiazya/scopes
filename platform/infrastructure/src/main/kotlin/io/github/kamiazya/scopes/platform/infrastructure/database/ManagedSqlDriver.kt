package io.github.kamiazya.scopes.platform.infrastructure.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * A managed wrapper around SqlDriver that ensures proper resource cleanup.
 *
 * This class implements AutoCloseable to ensure database connections are
 * properly closed when the application shuts down or when explicitly closed.
 *
 * @property databasePath The path to the SQLite database file
 * @property connectionProperties Optional JDBC connection properties
 *
 * @since 1.0.0
 */
class ManagedSqlDriver(private val databasePath: String, private val connectionProperties: Map<String, String> = emptyMap()) : AutoCloseable {

    private var _driver: SqlDriver? = null

    /**
     * The underlying SQL driver instance.
     * This is lazily initialized to avoid creating connections until needed.
     */
    val driver: SqlDriver
        get() = _driver ?: JdbcSqliteDriver(
            url = "jdbc:sqlite:$databasePath",
            properties = connectionProperties.toProperties(),
        ).also {
            _driver = it
            // Ensure the database schema is created if needed
            // This is handled by individual database implementations
        }

    /**
     * Checks if the driver has been initialized.
     */
    val isInitialized: Boolean
        get() = _driver != null

    /**
     * Closes the database connection.
     * Safe to call multiple times.
     */
    override fun close() {
        _driver?.let { driver ->
            try {
                driver.close()
            } catch (e: Exception) {
                // Log but don't rethrow - cleanup should not fail
                System.err.println("Failed to close database driver: ${e.message}")
            }
            _driver = null
        }
    }

    companion object {
        /**
         * Creates a managed SQL driver with default SQLite pragmas for better performance.
         */
        fun createWithDefaults(databasePath: String): ManagedSqlDriver = ManagedSqlDriver(
            databasePath = databasePath,
            connectionProperties = mapOf(
                // Enable foreign keys
                "foreign_keys" to "true",
                // Use WAL mode for better concurrency
                "journal_mode" to "WAL",
                // Synchronous mode for durability vs performance tradeoff
                "synchronous" to "NORMAL",
                // Cache size in pages (negative means KB)
                "cache_size" to "-64000",
                // Temp store in memory for better performance
                "temp_store" to "MEMORY",
            ),
        )
    }

    /**
     * Converts a map to Properties for JDBC.
     */
    private fun Map<String, String>.toProperties() = java.util.Properties().apply {
        this@toProperties.forEach { (key, value) -> setProperty(key, value) }
    }
}
