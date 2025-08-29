package io.github.kamiazya.scopes.platform.infrastructure.database

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for SQLite database connections.
 *
 * @property databasePath The path to the SQLite database file
 * @property walMode Enable Write-Ahead Logging mode for better concurrency
 * @property busyTimeout Timeout for busy database operations
 * @property synchronous Synchronous mode (FULL, NORMAL, OFF)
 * @property cacheSize Cache size in pages (-2000 = 2MB)
 * @property foreignKeys Enable foreign key constraints
 * @property journalMode Journal mode (DELETE, WAL, MEMORY)
 */
data class DatabaseConfiguration(
    val databasePath: String,
    val walMode: Boolean = true,
    val busyTimeout: Duration = 30.seconds,
    val synchronous: SynchronousMode = SynchronousMode.NORMAL,
    val cacheSize: Int = -2000,
    val foreignKeys: Boolean = true,
    val journalMode: JournalMode = JournalMode.WAL,
) {
    /**
     * SQLite synchronous modes.
     */
    enum class SynchronousMode {
        /** Maximum durability, slowest performance */
        FULL,

        /** Good balance of durability and performance */
        NORMAL,

        /** Fastest performance, risk of corruption on power loss */
        OFF,
    }

    /**
     * SQLite journal modes.
     */
    enum class JournalMode {
        /** Traditional rollback journal */
        DELETE,

        /** Write-Ahead Logging for better concurrency */
        WAL,

        /** In-memory journal, faster but less durable */
        MEMORY,
    }

    /**
     * Creates a JDBC URL from the configuration.
     */
    fun toJdbcUrl(): String = "jdbc:sqlite:$databasePath"

    /**
     * Checks if this is a test configuration.
     */
    fun isTest(): Boolean = databasePath == ":memory:"

    /**
     * Checks if this is a development configuration.
     */
    fun isDevelopment(): Boolean = synchronous == SynchronousMode.NORMAL && !isTest()

    /**
     * Gets PRAGMA statements to configure the database connection.
     */
    fun getPragmaStatements(): List<String> = buildList {
        if (walMode || journalMode == JournalMode.WAL) {
            add("PRAGMA journal_mode = WAL")
        } else {
            add("PRAGMA journal_mode = ${journalMode.name}")
        }
        add("PRAGMA busy_timeout = ${busyTimeout.inWholeMilliseconds}")
        add("PRAGMA synchronous = ${synchronous.name}")
        add("PRAGMA cache_size = $cacheSize")
        add("PRAGMA foreign_keys = ${if (foreignKeys) "ON" else "OFF"}")

        // Additional optimizations for SQLite
        add("PRAGMA temp_store = MEMORY")
        add("PRAGMA mmap_size = 30000000000") // 30GB memory-mapped I/O
    }

    companion object {
        /**
         * Creates a default configuration for development.
         */
        fun development(databasePath: String = "data/scopes.db"): DatabaseConfiguration = DatabaseConfiguration(
            databasePath = databasePath,
            walMode = true,
            synchronous = SynchronousMode.NORMAL,
        )

        /**
         * Creates a configuration optimized for production.
         */
        fun production(databasePath: String): DatabaseConfiguration = DatabaseConfiguration(
            databasePath = databasePath,
            walMode = true,
            synchronous = SynchronousMode.FULL,
            busyTimeout = 60.seconds,
        )

        /**
         * Creates a configuration for testing with in-memory database.
         */
        fun test(): DatabaseConfiguration = DatabaseConfiguration(
            databasePath = ":memory:",
            walMode = false,
            journalMode = JournalMode.MEMORY,
            synchronous = SynchronousMode.OFF,
            foreignKeys = true,
        )
    }
}
