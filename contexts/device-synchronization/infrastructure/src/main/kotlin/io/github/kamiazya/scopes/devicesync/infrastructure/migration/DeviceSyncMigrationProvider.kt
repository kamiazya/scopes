package io.github.kamiazya.scopes.devicesync.infrastructure.migration

import io.github.kamiazya.scopes.platform.infrastructure.database.migration.Migration
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.scanner.ResourceMigrationDiscovery
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Provides migrations for the Device Synchronization bounded context.
 */
class DeviceSyncMigrationProvider(private val logger: Logger) {
    private val discovery = ResourceMigrationDiscovery(
        resourcePath = "migrations/device-sync",
        classLoader = this::class.java.classLoader,
        logger = logger,
    )

    fun getMigrations(): List<Migration> = discovery.discoverMigrations()
}
