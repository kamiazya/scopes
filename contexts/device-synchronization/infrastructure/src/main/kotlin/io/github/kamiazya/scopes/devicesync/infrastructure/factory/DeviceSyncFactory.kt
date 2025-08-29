package io.github.kamiazya.scopes.devicesync.infrastructure.factory

/**
 * Factory for creating Device Synchronization instances.
 * @deprecated Use deviceSyncInfrastructureModule with Koin dependency injection
 */
@Deprecated(
    "Use deviceSyncInfrastructureModule with Koin dependency injection",
    ReplaceWith("deviceSyncInfrastructureModule", "io.github.kamiazya.scopes.devicesync.infrastructure.di.deviceSyncInfrastructureModule"),
)
object DeviceSyncFactory
