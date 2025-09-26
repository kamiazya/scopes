package io.github.kamiazya.scopes.platform.infrastructure.database.migration

/**
 * Represents errors that can occur during database migration operations.
 */
sealed class MigrationError : Exception() {

    /**
     * Migration file could not be found or loaded.
     */
    data class MigrationNotFound(val version: Long, val location: String) : MigrationError() {
        override val message = "Migration version $version not found at: $location"
    }

    /**
     * Migration version is out of order or conflicts with existing migrations.
     */
    data class VersionConflict(val version: Long, val conflictingVersion: Long, val reason: String) : MigrationError() {
        override val message = "Migration version $version conflicts with version $conflictingVersion: $reason"
    }

    /**
     * SQL execution failed during migration.
     */
    data class SqlExecutionError(val version: Long, val sql: String, override val cause: Throwable) : MigrationError() {
        override val message = "SQL execution failed for migration version $version: ${cause.message}"
    }

    /**
     * Database is in an inconsistent state and requires manual intervention.
     */
    data class CorruptedState(val reason: String, val suggestedAction: String) : MigrationError() {
        override val message = "Database migration state is corrupted: $reason. " +
            "Suggested action: $suggestedAction"
    }

    /**
     * Migration validation failed before execution.
     */
    data class ValidationError(val version: Long, val validationIssue: String) : MigrationError() {
        override val message = "Migration version $version failed validation: $validationIssue"
    }

    /**
     * Target version for rollback doesn't exist or is invalid.
     */
    data class InvalidTargetVersion(val targetVersion: Long, val currentVersion: Long, val reason: String) : MigrationError() {
        override val message = "Cannot migrate to version $targetVersion from $currentVersion: $reason"
    }

    /**
     * Generic database access error during migration.
     */
    data class DatabaseError(val operation: String, override val cause: Throwable) : MigrationError() {
        override val message = "Database error during $operation: ${cause.message}"
    }

    /**
     * Migration execution failed.
     */
    data class MigrationFailed(val version: Long, override val message: String, override val cause: Throwable? = null) : MigrationError()

    /**
     * No migrations were found.
     */
    object NoMigrationsFound : MigrationError() {
        override val message = "No migrations found"
    }

    /**
     * Schema is corrupted.
     */
    data class SchemaCorrupted(override val message: String, override val cause: Throwable? = null) : MigrationError()

    /**
     * Invalid migration structure or content.
     */
    data class InvalidMigration(val version: Long, override val message: String, override val cause: Throwable? = null) : MigrationError()

    /**
     * Version not found in applied migrations.
     */
    data class VersionNotFound(val version: Long) : MigrationError() {
        override val message = "Migration version $version not found in applied migrations"
    }
}
