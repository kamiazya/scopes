package io.github.kamiazya.scopes.scopemanagement.application.port

import arrow.core.Either
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError

/**
 * Port interface for projecting domain events to RDB storage.
 * 
 * This port abstracts the event projection functionality from the application layer,
 * allowing the infrastructure layer to provide the concrete implementation.
 * 
 * Follows the architectural pattern where:
 * - Events represent business decisions from the domain
 * - RDB remains the single source of truth for queries
 * - Events are projected to RDB in the same transaction
 * - Ensures read/write consistency
 */
interface EventProjector {

    /**
     * Project a single domain event to RDB storage.
     * This method should be called within the same transaction as event storage.
     * 
     * @param event The domain event to project
     * @return Either an application error or Unit on success
     */
    suspend fun projectEvent(event: DomainEvent): Either<ScopeManagementApplicationError, Unit>

    /**
     * Project multiple events in sequence.
     * All projections must succeed or the entire operation fails.
     * 
     * @param events The list of domain events to project
     * @return Either an application error or Unit on success
     */
    suspend fun projectEvents(events: List<DomainEvent>): Either<ScopeManagementApplicationError, Unit>
}