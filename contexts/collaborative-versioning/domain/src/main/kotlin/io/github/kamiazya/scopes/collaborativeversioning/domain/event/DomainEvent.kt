package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import kotlinx.datetime.Instant

/**
 * Base interface for domain events in the Collaborative Versioning context.
 *
 * Domain events represent facts that have occurred in the domain.
 * They are immutable and should contain all information necessary
 * to understand what happened.
 */
interface DomainEvent {
    /**
     * When this event occurred.
     */
    val occurredAt: Instant
}
