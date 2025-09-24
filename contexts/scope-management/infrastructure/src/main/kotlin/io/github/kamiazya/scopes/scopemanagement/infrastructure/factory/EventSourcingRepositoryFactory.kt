package io.github.kamiazya.scopes.scopemanagement.infrastructure.factory

import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.EventStoreContractErrorMapper
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.ContractBasedScopeEventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializersModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

/**
 * Factory for creating EventSourcingRepository instances.
 * This factory encapsulates the creation of repositories with their
 * internal dependencies, allowing the DI module to create instances
 * without exposing internal implementation classes.
 */
object EventSourcingRepositoryFactory {
    /**
     * Creates a contract-based event sourcing repository that uses
     * the EventStore contracts for cross-context communication.
     */
    fun createContractBased(
        eventStoreCommandPort: EventStoreCommandPort,
        eventStoreQueryPort: EventStoreQueryPort,
        logger: Logger,
        serializersModule: SerializersModule? = null,
    ): EventSourcingRepository<DomainEvent> {
        val errorMapper = EventStoreContractErrorMapper(logger)

        // Combine the provided serializers module with our scope events module
        val combinedModule = if (serializersModule != null) {
            serializersModule + ScopeEventSerializersModule.create()
        } else {
            ScopeEventSerializersModule.create()
        }

        val json = Json {
            this.serializersModule = combinedModule
            ignoreUnknownKeys = true
            isLenient = true
            classDiscriminator = "type"
        }

        return ContractBasedScopeEventSourcingRepository(
            eventStoreCommandPort = eventStoreCommandPort,
            eventStoreQueryPort = eventStoreQueryPort,
            eventStoreContractErrorMapper = errorMapper,
            json = json,
        )
    }
}
