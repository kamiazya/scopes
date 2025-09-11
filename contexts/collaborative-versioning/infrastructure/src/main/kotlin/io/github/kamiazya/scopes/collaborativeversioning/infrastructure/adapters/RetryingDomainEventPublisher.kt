package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.adapters

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventPublishingError
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = ConsoleLogger("RetryingDomainEventPublisher")

/**
 * Decorator that adds retry logic to a DomainEventPublisher.
 *
 * This implementation uses exponential backoff with jitter to retry
 * failed event publishing attempts, improving resilience against
 * transient failures.
 */
class RetryingDomainEventPublisher(
    private val delegate: DomainEventPublisher,
    private val maxAttempts: Int = 3,
    private val baseDelay: Duration = 100.milliseconds,
    private val maxDelay: Duration = 5.seconds,
    private val backoffFactor: Double = 2.0,
) : DomainEventPublisher {

    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(backoffFactor >= 1.0) { "backoffFactor must be >= 1.0" }
    }

    override suspend fun publish(event: DomainEvent): Either<EventPublishingError, Unit> = either {
        var lastError: EventPublishingError? = null

        repeat(maxAttempts) { attempt ->
            logger.debug(
                "Publishing event ${event.eventId} (attempt ${attempt + 1}/$maxAttempts)",
            )

            val publishResult = delegate.publish(event)
            publishResult.fold(
                { error ->
                    lastError = error
                    logger.warn(
                        "Failed to publish event ${event.eventId} on attempt ${attempt + 1}: $error",
                    )

                    if (attempt < maxAttempts - 1 && shouldRetry(error)) {
                        val delayMs = calculateBackoffDelay(attempt)
                        logger.debug(
                            "Retrying event ${event.eventId} after ${delayMs}ms",
                        )
                        delay(delayMs)
                    }
                },
                {
                    logger.debug(
                        "Successfully published event ${event.eventId} on attempt ${attempt + 1}",
                    )
                    return@either
                },
            )
        }

        // All attempts failed
        raise(
            lastError ?: EventPublishingError.InfrastructureError(
                message = "Failed to publish event after $maxAttempts attempts",
            ),
        )
    }

    override suspend fun publishAll(events: List<DomainEvent>): Either<EventPublishingError, Unit> = either {
        events.forEach { event ->
            publish(event).bind()
        }
    }

    private fun shouldRetry(error: EventPublishingError): Boolean = when (error) {
        // Don't retry serialization errors - they won't succeed on retry
        is EventPublishingError.SerializationFailed -> false

        // Don't retry unregistered event types - need to fix configuration
        is EventPublishingError.UnregisteredEventType -> false

        // Retry these transient errors
        is EventPublishingError.StorageFailed -> true
        is EventPublishingError.DistributionFailed -> true
        is EventPublishingError.PublishTimeout -> true
        is EventPublishingError.InfrastructureError -> true
    }

    private fun calculateBackoffDelay(attempt: Int): Duration {
        val exponentialDelay = baseDelay.inWholeMilliseconds * backoffFactor.pow(attempt)
        val jitteredDelay = exponentialDelay * (0.5 + kotlin.random.Random.nextDouble() * 0.5)
        val cappedDelay = jitteredDelay.coerceAtMost(maxDelay.inWholeMilliseconds.toDouble())

        return cappedDelay.toLong().milliseconds
    }
}
