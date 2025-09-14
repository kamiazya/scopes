package io.github.kamiazya.scopes.platform.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.domain.error.DomainError

/**
 * Version for optimistic concurrency control in aggregates.
 *
 * Versions start at 0 and increment with each change.
 * Used to detect concurrent modifications and prevent lost updates.
 */
@JvmInline
value class AggregateVersion private constructor(val value: Long) : Comparable<AggregateVersion> {

    /**
     * Increment the version by one.
     */
    fun increment(): AggregateVersion = AggregateVersion(value + 1)

    /**
     * Check if this version is the initial version.
     */
    fun isInitial(): Boolean = value == INITIAL_VERSION

    /**
     * Check if this version is after another version.
     */
    fun isAfter(other: AggregateVersion): Boolean = value > other.value

    /**
     * Check if this version is before another version.
     */
    fun isBefore(other: AggregateVersion): Boolean = value < other.value

    /**
     * Get the number of changes since initial version.
     */
    fun changeCount(): Long = value

    override fun compareTo(other: AggregateVersion): Int = value.compareTo(other.value)

    override fun toString(): String = "v$value"

    companion object {
        private const val INITIAL_VERSION = 0L
        private const val MIN_VERSION = 0L

        /**
         * Create the initial version (0).
         */
        fun initial(): AggregateVersion = AggregateVersion(INITIAL_VERSION)

        /**
         * Create a version from a specific value.
         */
        fun from(value: Long): Either<DomainError.InvalidVersion, AggregateVersion> = if (value >= MIN_VERSION) {
            AggregateVersion(value).right()
        } else {
            DomainError.InvalidVersion(
                value = value,
                errorType = DomainError.InvalidVersion.InvalidVersionType.NEGATIVE,
            ).left()
        }

        /**
         * Create a version from a specific value without validation.
         * Use with caution.
         */
        fun fromUnsafe(value: Long): AggregateVersion = AggregateVersion(value)

        /**
         * Calculate the version after applying a number of events.
         */
        fun afterEvents(eventCount: Int): Either<DomainError.InvalidVersion, AggregateVersion> = from(eventCount.toLong())
    }
}
