package io.github.kamiazya.scopes.apps.cli.di.eventstore

import io.github.kamiazya.scopes.contracts.eventstore.EventStorePort
import io.github.kamiazya.scopes.eventstore.application.adapter.EventStorePortAdapter
import io.github.kamiazya.scopes.eventstore.application.handler.GetEventsByAggregateHandler
import io.github.kamiazya.scopes.eventstore.application.handler.GetEventsSinceHandler
import io.github.kamiazya.scopes.eventstore.application.handler.StoreEventHandler
import io.github.kamiazya.scopes.eventstore.application.port.EventPublisher
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.db.EventStoreDatabase
import io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository
import io.github.kamiazya.scopes.eventstore.infrastructure.publisher.NoOpEventPublisher
import io.github.kamiazya.scopes.eventstore.infrastructure.repository.SqlDelightEventRepository
import io.github.kamiazya.scopes.eventstore.infrastructure.serializer.JsonEventSerializer
import io.github.kamiazya.scopes.eventstore.infrastructure.sqldelight.SqlDelightDatabaseProvider
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for Event Store infrastructure.
 *
 * This module provides:
 * - Table registration for database initialization
 * - Event serialization
 * - Repository implementation
 * - Application handlers
 * - Port adapter
 */
val eventStoreInfrastructureModule = module {
    // SQLDelight Database
    single<EventStoreDatabase>(named("eventStore")) {
        val databasePath: String = get<String>(named("databasePath"))
        SqlDelightDatabaseProvider.createDatabase("$databasePath/event-store.db")
    }

    // Event Serializer
    single<EventSerializer> {
        val serializersModule = getOrNull<SerializersModule>() ?: SerializersModule { }
        JsonEventSerializer(
            json = kotlinx.serialization.json.Json {
                this.serializersModule = serializersModule
                classDiscriminator = "type"
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
    }

    // Event Publisher
    single<EventPublisher> { NoOpEventPublisher() }

    // Repository (using SQLDelight implementation)
    single<EventRepository> {
        val database: EventStoreDatabase = get(named("eventStore"))
        SqlDelightEventRepository(
            queries = database.eventQueries,
            eventSerializer = get(),
        )
    }

    // Application Handlers
    single { StoreEventHandler(get(), get()) }
    single { GetEventsByAggregateHandler(get()) }
    single { GetEventsSinceHandler(get()) }

    // Port Adapter
    single<EventStorePort> {
        EventStorePortAdapter(
            storeEventHandler = get(),
            getEventsByAggregateHandler = get(),
            getEventsSinceHandler = get(),
            eventSerializer = get(),
        )
    }
}
