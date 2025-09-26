package io.github.kamiazya.scopes.scopemanagement.infrastructure.migration

import io.github.kamiazya.scopes.platform.infrastructure.database.migration.Migration
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.scanner.ResourceMigrationDiscovery
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Provides migrations for the Scope Management bounded context.
 *
 * Migrations are discovered from the classpath under the migrations directory.
 * They follow the naming convention: V{version}__{description}.sql
 */
class ScopeManagementMigrationProvider(private val logger: Logger) {
    private val discovery = ResourceMigrationDiscovery(
        resourcePath = "migrations/scope-management",
        classLoader = this::class.java.classLoader,
        logger = logger,
    )

    /**
     * Gets all migrations for the scope management context.
     */
    fun getMigrations(): List<Migration> = discovery.discoverMigrations()
}
