package io.github.kamiazya.scopes.platform.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.domain.error.DomainError

/**
 * Maps a Crockford Base32 character to its corresponding value.
 * Returns -1 for invalid characters.
 *
 * Crockford Base32 uses: 0123456789ABCDEFGHJKMNPQRSTVWXYZ
 * (excludes I, L, O, U to avoid confusion)
 */
private fun crockfordBase32Value(ch: Char): Int = when (ch) {
    '0' -> 0
    '1' -> 1
    '2' -> 2
    '3' -> 3
    '4' -> 4
    '5' -> 5
    '6' -> 6
    '7' -> 7
    '8' -> 8
    '9' -> 9
    'A', 'a' -> 10
    'B', 'b' -> 11
    'C', 'c' -> 12
    'D', 'd' -> 13
    'E', 'e' -> 14
    'F', 'f' -> 15
    'G', 'g' -> 16
    'H', 'h' -> 17
    'J', 'j' -> 18
    'K', 'k' -> 19
    'M', 'm' -> 20
    'N', 'n' -> 21
    'P', 'p' -> 22
    'Q', 'q' -> 23
    'R', 'r' -> 24
    'S', 's' -> 25
    'T', 't' -> 26
    'V', 'v' -> 27
    'W', 'w' -> 28
    'X', 'x' -> 29
    'Y', 'y' -> 30
    'Z', 'z' -> 31
    else -> -1
}

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
     *
     * ULID timestamp is encoded in the first 10 characters using Crockford Base32.
     * The 10 characters represent 50 bits, where the first 48 bits are the timestamp.
     */
    fun timestamp(): Long {
        // ULID timestamp is in the first 10 characters (50 bits total, 48 bits timestamp)
        if (value.length < 10) return 0L

        var timestamp = 0L
        for (i in 0 until 10) {
            val charValue = crockfordBase32Value(value[i])
            if (charValue == -1) return 0L // Invalid character
            timestamp = (timestamp shl 5) or charValue.toLong()
        }

        // Right-shift by 2 to drop the last 2 bits and get the 48-bit timestamp
        return timestamp shr 2
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
        fun from(value: String): Either<DomainError.InvalidEventId, EventId> = when {
            value.isEmpty() -> DomainError.InvalidEventId(
                value = value,
                errorType = DomainError.InvalidEventId.InvalidEventIdType.EMPTY,
            ).left()
            !ULID_PATTERN.matches(value) -> DomainError.InvalidEventId(
                value = value,
                errorType = DomainError.InvalidEventId.InvalidEventIdType.INVALID_UUID,
            ).left()
            else -> EventId(value).right()
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
