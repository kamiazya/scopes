package io.github.kamiazya.scopes.platform.infrastructure.database.migration

/**
 * Configuration options for database migration operations.
 *
 * @property maxRetries Maximum number of retry attempts for failed migrations
 */
data class MigrationConfig(val maxRetries: Int = 3)
