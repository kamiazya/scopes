package io.github.kamiazya.scopes.eventstore.domain.valueobject

/**
 * Value object representing an event type.
 *
 * Event types follow standard event sourcing patterns and use fully qualified class names
 * to ensure uniqueness across different packages and bounded contexts. This prevents
 * naming collisions and provides clear identification of event origins.
 *
 * @property value The fully qualified name of the event class (e.g., "io.github.kamiazya.scopes.domain.event.ScopeCreated")
 */
@JvmInline
value class EventType(val value: String) {
    init {
        require(value.isNotBlank()) { "Event type cannot be blank" }
    }
}
