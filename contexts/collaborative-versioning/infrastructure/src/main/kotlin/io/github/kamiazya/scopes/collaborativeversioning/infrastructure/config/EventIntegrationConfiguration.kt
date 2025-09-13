package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.config

import io.github.kamiazya.scopes.collaborativeversioning.application.handler.DomainEventHandlerRegistry
import io.github.kamiazya.scopes.collaborativeversioning.application.handler.ProposalCreatedHandler
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.infrastructure.adapters.EventStoreDomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.infrastructure.adapters.RetryingDomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.infrastructure.event.CollaborativeVersioningEventTypeRegistration
import io.github.kamiazya.scopes.collaborativeversioning.infrastructure.subscriber.EventStoreSubscriber
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.domain.model.EventTypeMapping
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger: Logger = ConsoleLogger("EventIntegrationConfiguration")

/**
 * Configuration for event integration in the collaborative versioning context.
 *
 * This class sets up all the necessary components for:
 * - Publishing domain events to the event store
 * - Subscribing to relevant events from other contexts
 * - Handling events within the collaborative versioning context
 */
class EventIntegrationConfiguration(
    private val eventStoreCommandPort: EventStoreCommandPort,
    private val eventStoreQueryPort: EventStoreQueryPort,
    private val eventSerializer: EventSerializer,
    private val eventTypeMapping: EventTypeMapping,
    private val coroutineScope: CoroutineScope,
) {

    /**
     * Configure and return the domain event publisher.
     */
    fun domainEventPublisher(): DomainEventPublisher {
        // Register event types
        CollaborativeVersioningEventTypeRegistration.registerAll(eventTypeMapping)

        // Create base publisher
        val basePublisher = EventStoreDomainEventPublisher(
            eventStoreCommandPort = eventStoreCommandPort,
            eventSerializer = eventSerializer,
            eventTypeMapping = eventTypeMapping,
            publishTimeout = 30.seconds,
        )

        // Wrap with retry logic
        return RetryingDomainEventPublisher(
            delegate = basePublisher,
            maxAttempts = 3,
            baseDelay = 100.milliseconds,
            maxDelay = 5.seconds,
            backoffFactor = 2.0,
        )
    }

    /**
     * Configure and return the event handler registry.
     */
    fun eventHandlerRegistry(): DomainEventHandlerRegistry {
        val registry = DomainEventHandlerRegistry()

        // Register all event handlers
        registry.register(ProposalCreatedHandler(logger))
        // Add more handlers as they are implemented:
        // registry.register(ProposalUpdatedHandler())
        // registry.register(ProposalReviewedHandler())
        // registry.register(ProposalApprovedHandler())
        // registry.register(ChangeMergedHandler())
        // registry.register(ConflictDetectedHandler())

        return registry
    }

    /**
     * Configure and return the event store subscriber.
     */
    fun eventStoreSubscriber(): EventStoreSubscriber = EventStoreSubscriber(
        eventStoreQueryPort = eventStoreQueryPort,
        eventSerializer = eventSerializer,
        handlerRegistry = eventHandlerRegistry(),
        pollingInterval = 5.seconds,
        coroutineScope = coroutineScope,
    )

    /**
     * Start the event subscriber with the collaborative versioning event types.
     */
    fun startEventSubscription() {
        val subscriber = eventStoreSubscriber()

        // Subscribe to all collaborative versioning events
        subscriber.start(
            eventTypes = CollaborativeVersioningEventTypeRegistration.ALL_TYPE_IDS,
        )

        // You could also subscribe to events from other contexts here
        // For example, if you need to react to scope management events:
        // subscriber.start(
        //     eventTypes = listOf("scope-management.scope.created.v1"),
        // )
    }
}
