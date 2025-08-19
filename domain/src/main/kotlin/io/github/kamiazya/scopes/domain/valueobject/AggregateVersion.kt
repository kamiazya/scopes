package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.AggregateVersionError
import kotlinx.datetime.Clock

/**
 * Value object representing the version of an aggregate.
 *
 * Used for optimistic concurrency control in event sourcing.
 * Version starts at 0 for new aggregates and increments with each event.
 *
 * Constraints:
 * - Must be non-negative
 * - Must increment sequentially (by 1)
 * - Has a maximum value to prevent overflow
 *
 * @property value The version number
 */
@JvmInline
value class AggregateVersion private constructor(val value: Int) {

    /**
     * Increments the version by 1.
     *
     * @return Either an error if overflow would occur, or the new version
     */
    fun increment(): Either<AggregateVersionError, AggregateVersion> {
        return if (value == MAX_VERSION) {
            AggregateVersionError.VersionOverflow(
                occurredAt = Clock.System.now(),
                currentVersion = value,
                maxVersion = MAX_VERSION
            ).left()
        } else {
            AggregateVersion(value + 1).right()
        }
    }

    /**
     * Validates a version transition.
     *
     * @param newVersion The version to transition to
     * @return Either an error if the transition is invalid, or Unit on success
     */
    fun validateTransition(newVersion: AggregateVersion): Either<AggregateVersionError, Unit> {
        return if (newVersion.value != value + 1) {
            AggregateVersionError.InvalidVersionTransition(
                occurredAt = Clock.System.now(),
                currentVersion = value,
                attemptedVersion = newVersion.value
            ).left()
        } else {
            Unit.right()
        }
    }

    /**
     * Checks if this version is the initial version (0).
     */
    fun isInitial(): Boolean = value == INITIAL_VERSION

    /**
     * Compares two versions.
     */
    operator fun compareTo(other: AggregateVersion): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    companion object {
        const val INITIAL_VERSION = 0
        const val MAX_VERSION = Int.MAX_VALUE - 1 // Reserve headroom to prevent overflow

        /**
         * The initial version for new aggregates.
         */
        val INITIAL = AggregateVersion(INITIAL_VERSION)

        /**
         * Creates an AggregateVersion from an Int.
         *
         * @param version The version number
         * @return Either an error or the AggregateVersion
         */
        fun create(version: Int): Either<AggregateVersionError, AggregateVersion> {
            return when {
                version < 0 -> AggregateVersionError.NegativeVersion(
                    occurredAt = Clock.System.now(),
                    attemptedVersion = version
                ).left()
                version > MAX_VERSION -> AggregateVersionError.VersionOverflow(
                    occurredAt = Clock.System.now(),
                    currentVersion = version,
                    maxVersion = MAX_VERSION
                ).left()
                else -> AggregateVersion(version).right()
            }
        }
    }
}
