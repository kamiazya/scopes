package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.platform.observability.Loggable
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.event.ActiveContextCleared
import io.github.kamiazya.scopes.scopemanagement.domain.event.ContextViewActivated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ContextViewApplied
import kotlinx.datetime.Clock

/**
 * Application service responsible for publishing audit events for context operations.
 * This service coordinates between the domain layer and infrastructure to publish
 * events that track context switches and usage for audit logging purposes.
 *
 * Moved to application layer as it primarily orchestrates infrastructure concerns
 * rather than encapsulating core domain logic.
 */
class ContextAuditService(private val eventPublisher: DomainEventPublisher) : Loggable {

    companion object {
        private const val DEFAULT_ERROR_MESSAGE = "Unknown error"
    }

    /**
     * Publish an event when a context view is activated.
     * This creates an audit trail of context switches.
     *
     * @param contextView The context view that was activated
     * @param previousContextId ID of the previously active context (if any)
     * @param activatedBy The user or system that activated the context
     * @return Either an error or Unit on success
     */
    suspend fun publishContextActivated(
        contextView: ContextView,
        previousContextId: String? = null,
        activatedBy: String? = null,
    ): Either<ApplicationError, Unit> {
        val eventId = EventId.generate()

        val aggregateId = contextView.id.toAggregateId().fold(
            {
                return PersistenceError.StorageUnavailable(
                    operation = "createAggregateId",
                    cause = "Failed to create aggregate ID: $it",
                ).left()
            },
            { it },
        )

        val previousContextViewId = previousContextId?.let { id ->
            when (val result = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId.create(id)) {
                is Either.Left -> return PersistenceError.StorageUnavailable(
                    operation = "parsePreviousContextId",
                    cause = "Invalid previous context ID: $id",
                ).left()
                is Either.Right -> result.value
            }
        }

        val event = ContextViewActivated(
            aggregateId = aggregateId,
            eventId = eventId,
            occurredAt = Clock.System.now(),
            aggregateVersion = io.github.kamiazya.scopes.platform.domain.value.AggregateVersion.initial().increment(),
            contextViewId = contextView.id,
            contextKey = contextView.key,
            contextName = contextView.name,
            previousContextId = previousContextViewId,
            activatedBy = activatedBy,
        )

        return try {
            eventPublisher.publish(event)
            logger.debug("Published context activated event for context: ${contextView.key.value}")
            Unit.right()
        } catch (e: Exception) {
            val error = PersistenceError.StorageUnavailable(
                operation = "publishContextActivated",
                cause = e.message ?: DEFAULT_ERROR_MESSAGE,
            )
            logger.error("Failed to publish context activated event for context: ${contextView.key.value}", throwable = e)
            error.left()
        }
    }

    /**
     * Publish an event when the active context is cleared.
     * This tracks when users disable filtering.
     *
     * @param previousContext The context view that was previously active
     * @param clearedBy The user or system that cleared the context
     * @return Either an error or Unit on success
     */
    suspend fun publishActiveContextCleared(previousContext: ContextView, clearedBy: String? = null): Either<ApplicationError, Unit> {
        val eventId = EventId.generate()

        val aggregateId = previousContext.id.toAggregateId().fold(
            {
                return PersistenceError.StorageUnavailable(
                    operation = "createAggregateId",
                    cause = "Failed to create aggregate ID: $it",
                ).left()
            },
            { it },
        )

        val event = ActiveContextCleared(
            aggregateId = aggregateId,
            eventId = eventId,
            occurredAt = Clock.System.now(),
            aggregateVersion = io.github.kamiazya.scopes.platform.domain.value.AggregateVersion.initial().increment(),
            previousContextId = previousContext.id,
            previousContextKey = previousContext.key,
            clearedBy = clearedBy,
        )

        return try {
            eventPublisher.publish(event)
            logger.debug("Published active context cleared event for previous context: ${previousContext.key.value}")
            Unit.right()
        } catch (e: Exception) {
            val error = PersistenceError.StorageUnavailable(
                operation = "publishActiveContextCleared",
                cause = e.message ?: DEFAULT_ERROR_MESSAGE,
            )
            logger.error("Failed to publish active context cleared event for context: ${previousContext.key.value}", throwable = e)
            error.left()
        }
    }

    /**
     * Publish an event when a context view is applied to filter scopes.
     * This provides audit trail for context usage without switching active context.
     *
     * @param contextView The context view that was applied
     * @param scopeCount Number of scopes returned after filtering
     * @param totalScopeCount Total number of scopes before filtering
     * @param appliedBy The user or system that applied the filter
     * @return Either an error or Unit on success
     */
    suspend fun publishContextApplied(
        contextView: ContextView,
        scopeCount: Int,
        totalScopeCount: Int,
        appliedBy: String? = null,
    ): Either<ApplicationError, Unit> {
        val eventId = EventId.generate()

        val aggregateId = contextView.id.toAggregateId().fold(
            {
                return PersistenceError.StorageUnavailable(
                    operation = "createAggregateId",
                    cause = "Failed to create aggregate ID: $it",
                ).left()
            },
            { it },
        )

        val event = ContextViewApplied(
            aggregateId = aggregateId,
            eventId = eventId,
            occurredAt = Clock.System.now(),
            aggregateVersion = io.github.kamiazya.scopes.platform.domain.value.AggregateVersion.initial().increment(),
            contextViewId = contextView.id,
            contextKey = contextView.key,
            scopeCount = scopeCount,
            totalScopeCount = totalScopeCount,
            appliedBy = appliedBy,
        )

        return try {
            eventPublisher.publish(event)
            logger.debug("Published context applied event for context: ${contextView.key.value}, filtered $scopeCount/$totalScopeCount scopes")
            Unit.right()
        } catch (e: Exception) {
            val error = PersistenceError.StorageUnavailable(
                operation = "publishContextApplied",
                cause = e.message ?: DEFAULT_ERROR_MESSAGE,
            )
            logger.error("Failed to publish context applied event for context: ${contextView.key.value}", throwable = e)
            error.left()
        }
    }
}
