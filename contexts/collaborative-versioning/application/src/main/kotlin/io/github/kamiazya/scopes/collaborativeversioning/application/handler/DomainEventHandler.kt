package io.github.kamiazya.scopes.collaborativeversioning.application.handler

import arrow.core.Either
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventHandlingError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import kotlin.reflect.KClass

/**
 * Base interface for domain event handlers.
 *
 * Event handlers are responsible for reacting to domain events and
 * executing side effects or updating read models.
 */
interface DomainEventHandler<T : DomainEvent> {

    /**
     * The type of event this handler processes.
     */
    val eventType: KClass<T>

    /**
     * Handle the domain event.
     *
     * @param event The event to handle
     * @return Either an error if handling failed, or Unit if successful
     */
    suspend fun handle(event: T): Either<EventHandlingError, Unit>

    /**
     * Whether this handler can process the given event.
     * Default implementation checks event type.
     */
    fun canHandle(event: DomainEvent): Boolean = eventType.isInstance(event)
}

/**
 * Registry for domain event handlers.
 *
 * Manages the registration and dispatch of events to appropriate handlers.
 */
class DomainEventHandlerRegistry {
    private val handlers = mutableListOf<DomainEventHandler<*>>()

    /**
     * Register a handler for processing events.
     */
    fun <T : DomainEvent> register(handler: DomainEventHandler<T>) {
        handlers.add(handler)
    }

    /**
     * Get all handlers that can process the given event.
     */
    fun getHandlersFor(event: DomainEvent): List<DomainEventHandler<DomainEvent>> = handlers
        .filter { it.canHandle(event) }
        .map {
            @Suppress("UNCHECKED_CAST")
            it as DomainEventHandler<DomainEvent>
        }

    /**
     * Dispatch an event to all registered handlers.
     *
     * @param event The event to dispatch
     * @return Either an error if any handler failed, or Unit if all successful
     */
    suspend fun dispatch(event: DomainEvent): Either<EventHandlingError, Unit> = Either.catch {
        val eventHandlers = getHandlersFor(event)

        if (eventHandlers.isEmpty()) {
            return@catch
        }

        // Execute all handlers and collect errors
        val errors = mutableListOf<EventHandlingError>()

        eventHandlers.forEach { handler ->
            handler.handle(event).fold(
                { error -> errors.add(error) },
                { /* Success - continue */ },
            )
        }

        if (errors.isNotEmpty()) {
            error("Failed to handle events: ${errors.joinToString("; ")}")
        }
    }.fold(
        { throwable ->
            when (throwable) {
                is EventHandlingException ->
                    Either.Left(EventHandlingError.MultipleHandlersFailed(throwable.errors))
                else ->
                    Either.Left(
                        EventHandlingError.UnexpectedError(
                            message = "Unexpected error during event dispatch",
                            cause = throwable,
                        ),
                    )
            }
        },
        { Either.Right(it) },
    )
}

/**
 * Exception thrown when event handling fails.
 */
private class EventHandlingException(val errors: List<EventHandlingError>) : Exception("Event handling failed with ${errors.size} errors")
