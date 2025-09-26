package io.github.kamiazya.scopes.eventstore.infrastructure.migration

import io.github.kamiazya.scopes.platform.infrastructure.database.migration.Migration
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.scanner.ResourceMigrationDiscovery
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Provides migrations for the Event Store bounded context.
 *
 * Migrations are discovered from classpath resources under migrations/event-store.
 */
class EventStoreMigrationProvider(private val logger: Logger) {
    private val discovery = ResourceMigrationDiscovery(
        resourcePath = "migrations/event-store",
        classLoader = this::class.java.classLoader,
        logger = logger,
    )

    fun getMigrations(): List<Migration> = discovery.discoverMigrations()
}
