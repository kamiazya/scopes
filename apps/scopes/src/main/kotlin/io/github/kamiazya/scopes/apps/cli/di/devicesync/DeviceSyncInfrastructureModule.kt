package io.github.kamiazya.scopes.apps.cli.di.devicesync

import io.github.kamiazya.scopes.contracts.devicesync.DeviceSynchronizationCommandPort
import io.github.kamiazya.scopes.devicesync.application.adapter.DeviceSynchronizationCommandPortAdapter
import io.github.kamiazya.scopes.devicesync.application.handler.command.SynchronizeDeviceHandler
import io.github.kamiazya.scopes.devicesync.application.port.EventCommandPort
import io.github.kamiazya.scopes.devicesync.application.port.EventQueryPort
import io.github.kamiazya.scopes.devicesync.db.DeviceSyncDatabase
import io.github.kamiazya.scopes.devicesync.domain.repository.SynchronizationRepository
import io.github.kamiazya.scopes.devicesync.domain.service.DeviceSynchronizationService
import io.github.kamiazya.scopes.devicesync.infrastructure.adapters.EventStoreEventAppender
import io.github.kamiazya.scopes.devicesync.infrastructure.adapters.EventStoreEventReader
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

    // Event Query (Adapter)
    single<EventQueryPort> {
        EventStoreEventReader(
            eventStoreQueryPort = get(),
            logger = get(),
            json = get(),
        )
    }

    // Event Command (Adapter)
    single<EventCommandPort> {
        EventStoreEventAppender(
            eventStoreCommandPort = get(),
            logger = get(),
            json = get(),
        )
    }

    // Domain Service
    single<DeviceSynchronizationService> {
        DefaultDeviceSynchronizationService(
            syncRepository = get(),
            eventReader = get<EventQueryPort>(),
        )
    }

    // Application Handlers
    single { SynchronizeDeviceHandler(get(), get()) }

    // Port Adapter
    single<DeviceSynchronizationCommandPort> {
        DeviceSynchronizationCommandPortAdapter(
            synchronizeHandler = get(),
            syncRepository = get(),
        )
    }
}
