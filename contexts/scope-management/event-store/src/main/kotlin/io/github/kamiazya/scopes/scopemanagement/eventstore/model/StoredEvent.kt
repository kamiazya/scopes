package io.github.kamiazya.scopes.scopemanagement.eventstore.model

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent

/**
 * Represents a domain event that has been stored with its metadata.
 *
 * This combines the original domain event with storage-specific metadata
 * needed for synchronization and conflict resolution.
 */
data class StoredEvent(val metadata: EventMetadata, val event: DomainEvent) {
    /**
     * Checks if this event happened before another based on vector clocks.
     */
    fun happenedBefore(other: StoredEvent): Boolean = metadata.vectorClock.happenedBefore(other.metadata.vectorClock)

    /**
     * Checks if this event is concurrent with another based on vector clocks.
     */
    fun isConcurrentWith(other: StoredEvent): Boolean = metadata.vectorClock.isConcurrentWith(other.metadata.vectorClock)
}
