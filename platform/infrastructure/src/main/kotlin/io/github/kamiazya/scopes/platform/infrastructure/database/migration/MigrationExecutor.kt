package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import arrow.core.Either

/**
 * Interface for executing database migrations.
 *
 * Provides low-level database access for migration operations with
 * transaction management and error handling.
 */
interface MigrationExecutor {

    /**
     * Execute a list of SQL statements within a transaction.
     * All statements must succeed or the entire transaction is rolled back.
     *
     * @param sqlStatements List of SQL statements to execute
     * @return Either an error or Unit on success
     */
    suspend fun executeSql(sqlStatements: List<String>): Either<MigrationError, Unit>

    /**
     * Execute a single SQL statement.
     *
     * @param sql The SQL statement to execute
     * @return Either an error or Unit on success
     */
    suspend fun executeSql(sql: String): Either<MigrationError, Unit> = executeSql(listOf(sql))

    /**
     * Check if a table exists in the database.
     *
     * @param tableName The name of the table to check
     * @return Either an error or true if table exists, false otherwise
     */
    suspend fun tableExists(tableName: String): Either<MigrationError, Boolean>

    /**
     * Get the current database schema version.
     * Returns 0 if no migrations have been applied.
     *
     * @return Either an error or the current version number
     */
    suspend fun getCurrentVersion(): Either<MigrationError, Long>

    /**
     * Check if the schema_versions table exists and create it if necessary.
     * This is called automatically before any migration operations.
     *
     * @return Either an error or Unit on success
     */
    suspend fun ensureSchemaVersionsTable(): Either<MigrationError, Unit>

    /**
     * Validate the database connection and basic operations.
     *
     * @return Either an error or Unit if database is accessible
     */
    suspend fun validateConnection(): Either<MigrationError, Unit>
}

/**
 * SQLDelight-based implementation of MigrationExecutor.
 *
 * Provides transaction management and error handling for SQLite databases
 * using the existing SQLDelight infrastructure.
 */
class SqlDelightMigrationExecutor(private val driver: SqlDriver) : MigrationExecutor {

    override suspend fun executeSql(sqlStatements: List<String>): Either<MigrationError, Unit> {
        if (sqlStatements.isEmpty()) return Either.Right(Unit)

        // Fast-path: single non-empty statement; let SQLite handle atomicity
        if (sqlStatements.size == 1) {
            val sql = sqlStatements.first().trim()
            if (sql.isEmpty()) return Either.Right(Unit)
            return try {
                driver.execute(null, sql, 0)
                Either.Right(Unit)
            } catch (e: Exception) {
                Either.Left(
                    MigrationError.SqlExecutionError(
                        version = 0,
                        sql = sql,
                        cause = e,
                    ),
                )
            }
        }

        // Multiple statements: execute atomically using SQLDelight Transacter
        val db = io.github.kamiazya.scopes.platform.db.PlatformDatabase(driver)
        var currentSql: String = ""
        return try {
            db.transaction(noEnclosing = true) {
                for (stmt in sqlStatements) {
                    val s = stmt.trim()
                    if (s.isEmpty()) continue
                    currentSql = s
                    driver.execute(null, s, 0)
                }
            }
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(
                MigrationError.SqlExecutionError(
                    version = 0,
                    sql = currentSql.ifEmpty { "<unknown>" },
                    cause = e,
                ),
            )
        }
    }

    override suspend fun tableExists(tableName: String): Either<MigrationError, Boolean> = try {
        val query = """
                SELECT COUNT(*) as count
                FROM sqlite_master
                WHERE type='table' AND name=?
        """.trimIndent()

        var exists = false
        driver.executeQuery(null, query, { cursor ->
            if (cursor.next().value) {
                exists = cursor.getLong(0)?.let { it > 0 } ?: false
            }
            QueryResult.Value(Unit)
        }, 1) {
            bindString(0, tableName)
        }

        Either.Right(exists)
    } catch (e: Exception) {
        Either.Left(
            MigrationError.DatabaseError(
                operation = "check table existence",
                cause = e,
            ),
        )
    }

    override suspend fun getCurrentVersion(): Either<MigrationError, Long> = ensureSchemaVersionsTable().fold(
        ifLeft = { Either.Left(it) },
        ifRight = {
            try {
                val query = "SELECT COALESCE(MAX(version), 0) as version FROM schema_versions"
                var version = 0L

                driver.executeQuery(null, query, { cursor ->
                    if (cursor.next().value) {
                        version = cursor.getLong(0) ?: 0L
                    }
                    QueryResult.Value(Unit)
                }, 0)

                Either.Right(version)
            } catch (e: Exception) {
                Either.Left(
                    MigrationError.DatabaseError(
                        operation = "get current version",
                        cause = e,
                    ),
                )
            }
        },
    )

    override suspend fun ensureSchemaVersionsTable(): Either<MigrationError, Unit> {
        val createTableSql = """
            CREATE TABLE IF NOT EXISTS schema_versions (
                version INTEGER PRIMARY KEY NOT NULL,
                description TEXT NOT NULL,
                applied_at INTEGER NOT NULL,
                execution_time_ms INTEGER NOT NULL
            )
        """.trimIndent()

        return try {
            driver.execute(null, createTableSql, 0)
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(
                MigrationError.DatabaseError(
                    operation = "create schema_versions table",
                    cause = e,
                ),
            )
        }
    }

    override suspend fun validateConnection(): Either<MigrationError, Unit> = try {
        driver.executeQuery(null, "SELECT 1", { QueryResult.Value(Unit) }, 0)
        Either.Right(Unit)
    } catch (e: Exception) {
        Either.Left(
            MigrationError.DatabaseError(
                operation = "validate connection",
                cause = e,
            ),
        )
    }
}
