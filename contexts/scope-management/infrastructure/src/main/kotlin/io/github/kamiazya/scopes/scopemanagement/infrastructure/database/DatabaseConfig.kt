package io.github.kamiazya.scopes.scopemanagement.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

/**
 * Database configuration for connection pooling and performance optimization.
 */
object DatabaseConfig {

    /**
     * Creates a HikariCP connection pool for SQLite database.
     *
     * @param jdbcUrl The JDBC URL for the SQLite database
     * @param poolSize The maximum number of connections in the pool (default: 10)
     * @return Configured HikariDataSource
     */
    fun createConnectionPool(jdbcUrl: String = "jdbc:sqlite:scopes.db", poolSize: Int = 10): HikariDataSource {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = "org.sqlite.JDBC"

            // Connection pool settings
            maximumPoolSize = poolSize
            minimumIdle = 2
            idleTimeout = 600000 // 10 minutes
            connectionTimeout = 30000 // 30 seconds
            maxLifetime = 1800000 // 30 minutes

            // SQLite specific optimizations
            addDataSourceProperty("journal_mode", "WAL")
            addDataSourceProperty("cache_size", "-64000") // 64MB cache
            addDataSourceProperty("synchronous", "NORMAL")
            addDataSourceProperty("temp_store", "MEMORY")
            addDataSourceProperty("mmap_size", "268435456") // 256MB memory-mapped I/O

            // Performance optimizations
            addDataSourceProperty("foreign_keys", "ON")
            addDataSourceProperty("recursive_triggers", "ON")

            // Connection test query
            connectionTestQuery = "SELECT 1"

            // Pool name for monitoring
            poolName = "ScopeManagementPool"
        }

        return HikariDataSource(config)
    }

    /**
     * SQLite performance tuning statements to execute on each connection.
     */
    val performanceTuningStatements = listOf(
        "PRAGMA journal_mode = WAL",
        "PRAGMA cache_size = -64000",
        "PRAGMA synchronous = NORMAL",
        "PRAGMA temp_store = MEMORY",
        "PRAGMA mmap_size = 268435456",
        "PRAGMA page_size = 4096",
        "PRAGMA optimize",
    )
}
