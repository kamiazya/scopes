package io.github.kamiazya.scopes.eventstore.application.port

import io.github.kamiazya.scopes.eventstore.domain.entity.PersistedEventRecord

/**
 * Port for publishing events after they are stored.
 *
 * This interface defines the contract for event publishers,
 * allowing different implementations (e.g., in-memory, message queue, etc.).
 */
fun interface EventPublisher {
    /**
     * Publishes a stored event.
     *
     * @param event The event to publish
     */
    suspend fun publish(event: PersistedEventRecord)
}
