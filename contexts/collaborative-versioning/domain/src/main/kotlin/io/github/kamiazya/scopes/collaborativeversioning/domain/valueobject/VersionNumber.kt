package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.VersionNumberError

/**
 * Value object representing a version number in the system.
 *
 * Version numbers are strictly positive integers that increment
 * monotonically for each resource. They provide an ordering mechanism
 * for the evolution of a resource over time.
 */
@JvmInline
value class VersionNumber private constructor(val value: Int) {

    companion object {
        /**
         * The initial version number for new resources.
         */
        val INITIAL = VersionNumber(1)

        /**
         * Create a VersionNumber from an integer value.
         *
         * @param value The version number value
         * @return Either<VersionNumberError, VersionNumber>
         */
        fun from(value: Int): Either<VersionNumberError, VersionNumber> = either {
            ensure(value > 0) {
                VersionNumberError.InvalidValue(
                    providedValue = value,
                    reason = "Version number must be positive",
                )
            }
            VersionNumber(value)
        }

        /**
         * Create an unsafe VersionNumber (for testing purposes).
         *
         * @throws IllegalArgumentException if value is not positive
         */
        internal fun unsafe(value: Int): VersionNumber {
            require(value > 0) { "Version number must be positive" }
            return VersionNumber(value)
        }
    }

    /**
     * Increment this version number to the next version.
     */
    fun increment(): VersionNumber = VersionNumber(value + 1)

    /**
     * Check if this version is the initial version.
     */
    fun isInitial(): Boolean = value == 1

    /**
     * Check if this version comes before another version.
     */
    fun isBefore(other: VersionNumber): Boolean = value < other.value

    /**
     * Check if this version comes after another version.
     */
    fun isAfter(other: VersionNumber): Boolean = value > other.value

    /**
     * Calculate the difference between this version and another.
     * Returns positive value if this version is newer.
     */
    fun distanceFrom(other: VersionNumber): Int = value - other.value

    override fun toString(): String = "v$value"
}
