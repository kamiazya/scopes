package io.github.kamiazya.scopes.apps.cli.di.devicesync

import io.github.kamiazya.scopes.contracts.devicesync.DeviceSynchronizationPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStorePort
import io.github.kamiazya.scopes.devicesync.application.adapter.DeviceSyncPortAdapter
import io.github.kamiazya.scopes.devicesync.application.handler.SynchronizeDeviceHandler
import io.github.kamiazya.scopes.devicesync.db.DeviceSyncDatabase
import io.github.kamiazya.scopes.devicesync.domain.repository.SynchronizationRepository
import io.github.kamiazya.scopes.devicesync.domain.service.DeviceSynchronizationService
import io.github.kamiazya.scopes.devicesync.infrastructure.repository.SqlDelightSynchronizationRepository
import io.github.kamiazya.scopes.devicesync.infrastructure.service.DefaultDeviceSynchronizationService
import io.github.kamiazya.scopes.devicesync.infrastructure.sqldelight.SqlDelightDatabaseProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for Device Synchronization infrastructure.
 *
 * This module provides:
 * - Table registration for database initialization
 * - Repository implementation
 * - Domain service implementation
 * - Application handlers
 * - Port adapter
 */
val deviceSyncInfrastructureModule = module {
    // SQLDelight Database
    single<DeviceSyncDatabase>(named("deviceSync")) {
        val databasePath: String = get<String>(named("databasePath"))
        SqlDelightDatabaseProvider.createDatabase("$databasePath/device-sync.db")
    }

    // Repository
    single<SynchronizationRepository> {
        val database: DeviceSyncDatabase = get(named("deviceSync"))
        SqlDelightSynchronizationRepository(
            deviceQueries = database.deviceQueries,
            vectorClockQueries = database.vectorClockQueries,
        )
    }

    // Domain Service
    single<DeviceSynchronizationService> {
        DefaultDeviceSynchronizationService(
            syncRepository = get(),
            eventStore = get<EventStorePort>(),
        )
    }

    // Application Handlers
    single { SynchronizeDeviceHandler(get(), get()) }

    // Port Adapter
    single<DeviceSynchronizationPort> {
        DeviceSyncPortAdapter(
            synchronizeHandler = get(),
            syncRepository = get(),
        )
    }
}
