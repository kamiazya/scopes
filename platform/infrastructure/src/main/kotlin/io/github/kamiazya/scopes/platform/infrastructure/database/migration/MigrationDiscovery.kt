package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import arrow.core.Either

/**
 * Result of migration discovery operation.
 */
data class DiscoveryReport(
    val migrations: List<Migration>,
    val discoveredFiles: Int,
    val validMigrations: Int,
    val invalidMigrations: List<ValidationError>,
    val duplicateVersions: List<Long>,
)

/**
 * Validation error for discovered migrations.
 */
data class ValidationError(val file: String, val version: Long?, val reason: String, val cause: Throwable? = null)

/**
 * Service for discovering and validating database migration files.
 *
 * This service scans specified directories for migration files,
 * validates their format and content, and returns a list of
 * executable Migration objects.
 */
interface MigrationDiscovery {

    /**
     * Discover migrations from specified directories.
     *
     * @param searchPaths List of directories to search for migrations
     * @param recursive Whether to search subdirectories recursively
     * @return Either an error or discovery result
     */
    suspend fun discoverMigrations(searchPaths: List<String>, recursive: Boolean = true): Either<MigrationError, DiscoveryReport>

    /**
     * Discover migrations from a single directory.
     *
     * @param searchPath Directory to search for migrations
     * @param recursive Whether to search subdirectories recursively
     * @return Either an error or discovery result
     */
    suspend fun discoverMigrations(searchPath: String, recursive: Boolean = true): Either<MigrationError, DiscoveryReport> =
        discoverMigrations(listOf(searchPath), recursive)

    /**
     * Load and validate a specific migration file.
     *
     * @param filePath Path to the migration file
     * @return Either an error or the loaded migration
     */
    suspend fun loadMigration(filePath: String): Either<MigrationError, Migration>

    /**
     * Validate the discovered migrations for consistency.
     *
     * @param migrations List of migrations to validate
     * @return Either an error or validation result
     */
    suspend fun validateMigrations(migrations: List<Migration>): Either<MigrationError, List<ValidationError>>
}
