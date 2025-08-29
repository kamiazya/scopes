package io.github.kamiazya.scopes.eventstore.domain.valueobject

/**
 * Value object representing an event type.
 *
 * @property value The string representation of the event type (usually the class name)
 */
@JvmInline
value class EventType(val value: String) {
    init {
        require(value.isNotBlank()) { "Event type cannot be blank" }
    }
}
