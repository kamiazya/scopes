package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Abstract base class for command handlers that use Event Sourcing pattern.
 *
 * This class provides common functionality for loading aggregates from event streams
 * and handling the boilerplate of event sourcing operations. It reduces code duplication
 * across concrete event sourcing handlers.
 *
 * @property eventSourcingRepository Repository for loading and saving events
 * @property applicationErrorMapper Maps domain errors to contract errors
 * @property logger Logger for diagnostic output
 */
abstract class AbstractEventSourcingHandler(
    protected val eventSourcingRepository: EventSourcingRepository<DomainEvent>,
    protected val applicationErrorMapper: ApplicationErrorMapper,
    protected val logger: Logger,
) {
    /**
     * Loads an existing aggregate from the event store.
     *
     * This method handles the common pattern of:
     * 1. Parsing and validating the scope ID
     * 2. Converting to aggregate ID
     * 3. Loading events from the repository
     * 4. Reconstructing the aggregate from events
     * 5. Handling the case where the aggregate doesn't exist
     *
     * @param scopeIdString The string representation of the scope ID
     * @return Either an error or the loaded aggregate
     */
    protected suspend fun loadExistingAggregate(scopeIdString: String): Either<ScopeContractError, ScopeAggregate> = either {
        // Parse scope ID
        val scopeId = ScopeId.create(scopeIdString).mapLeft { idError ->
            logger.warn("Invalid scope ID format", mapOf("scopeId" to scopeIdString))
            applicationErrorMapper.mapDomainError(
                idError,
                ErrorMappingContext(attemptedValue = scopeIdString),
            )
        }.bind()

        // Load current aggregate from events
        val aggregateId = scopeId.toAggregateId().mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        val events = eventSourcingRepository.getEvents(aggregateId).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        // Reconstruct aggregate from events using fromEvents method
        val scopeEvents = events.filterIsInstance<io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent>()
        val baseAggregate = ScopeAggregate.fromEvents(scopeEvents).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        baseAggregate ?: run {
            logger.warn("Scope not found", mapOf("scopeId" to scopeIdString))
            raise(
                applicationErrorMapper.mapDomainError(
                    io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError.NotFound(scopeId),
                    ErrorMappingContext(attemptedValue = scopeIdString),
                ),
            )
        }
    }
}
