package io.github.kamiazya.scopes.platform.domain.event

import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion

/**
 * Support interface for events that can be assigned a version.
 *
 * This interface provides a type-safe way to assign versions to events,
 * avoiding the need for reflection-based approaches. It ensures that
 * version assignment is handled consistently across all event types.
 *
 * @param T The concrete event type that implements this interface
 */
interface VersionSupport<T : DomainEvent> {
    /**
     * Creates a copy of this event with the specified version.
     *
     * @param version The new version to assign to the event
     * @return A new instance of this event with the updated version
     */
    fun withVersion(version: AggregateVersion): T
}
