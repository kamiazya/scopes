package io.github.kamiazya.scopes.platform.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.domain.error.DomainError

/**
 * Unique identifier for domain events.
 *
 * Uses ULID (Universally Unique Lexicographically Sortable Identifier)
 * to provide time-ordered, globally unique event IDs.
 */
@JvmInline
value class EventId private constructor(val value: String) : Comparable<EventId> {

    /**
     * Extract the timestamp component from the ULID.
     * Returns the milliseconds since Unix epoch.
     */
    fun timestamp(): Long {
        // ULID timestamp is in the first 10 characters (48 bits)
        // This is a simplified extraction - in production, use a proper ULID library
        return value.substring(0, 10).toLongOrNull(32) ?: 0L
    }

    /**
     * Compare events by their natural ULID ordering (time-based).
     */
    override fun compareTo(other: EventId): Int = value.compareTo(other.value)

    override fun toString(): String = value

    companion object {
        private val ULID_PATTERN = Regex("^[0-9A-HJKMNP-TV-Z]{26}$")

        /**
         * Generate a new event ID with the current timestamp.
         */
        fun generate(): EventId = EventId(ULID.generate().value)

        /**
         * Create an event ID from an existing ULID string.
         */
        fun from(value: String): Either<DomainError.InvalidEventId, EventId> = if (ULID_PATTERN.matches(value)) {
            EventId(value).right()
        } else {
            DomainError.InvalidEventId(
                value = value,
                reason = "Invalid ULID format for event ID",
            ).left()
        }

        /**
         * Create an event ID without validation.
         * Use with caution.
         */
        fun fromUnsafe(value: String): EventId = EventId(value)

        /**
         * Parse a string that might be an event ID.
         * Returns null if invalid rather than throwing.
         */
        fun tryParse(value: String): EventId? = from(value).getOrNull()
    }
}
