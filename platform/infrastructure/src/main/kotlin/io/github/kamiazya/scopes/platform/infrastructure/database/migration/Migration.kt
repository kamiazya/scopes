package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import arrow.core.Either
import io.github.kamiazya.scopes.platform.commons.time.Instant
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents a database schema migration.
 *
 * Each migration is uniquely identified by its version and can be applied
 * in ascending order to upgrade the database schema.
 */
interface Migration {
    /**
     * The version number of this migration.
     * Versions should be sequential and unique.
     */
    val version: Long

    /**
     * Human-readable description of what this migration does.
     */
    val description: String

    /**
     * Apply this migration to the database.
     *
     * @param executor The migration executor that provides database access
     * @return Either an error or Unit on success
     */
    suspend fun apply(executor: MigrationExecutor): Either<MigrationError, Unit>
}

/**
 * Abstract base class for SQL-based migrations.
 * Provides common functionality for most database schema changes.
 */
abstract class SqlMigration(override val version: Long, override val description: String) : Migration {

    /**
     * SQL statements to execute when applying this migration.
     */
    abstract val sql: List<String>

    override suspend fun apply(executor: MigrationExecutor): Either<MigrationError, Unit> = executor.executeSql(sql)
}

/**
 * Represents an applied migration record stored in the database.
 */
data class AppliedMigration(val version: Long, val description: String, val appliedAt: Instant, val executionTime: Duration) {
    companion object {
        fun from(migration: Migration, appliedAt: Instant = Clock.System.now(), executionTimeMs: Long): AppliedMigration = AppliedMigration(
            version = migration.version,
            description = migration.description,
            appliedAt = appliedAt,
            executionTime = executionTimeMs.milliseconds,
        )
    }
}
