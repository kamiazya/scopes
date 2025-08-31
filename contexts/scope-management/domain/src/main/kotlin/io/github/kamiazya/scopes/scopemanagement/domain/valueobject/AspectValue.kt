package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValueError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Value object representing an aspect value.
 * Aspects are key-value pairs that provide metadata about a scope.
 */
@JvmInline
value class AspectValue private constructor(val value: String) {
    companion object {
        private const val MAX_LENGTH = 500

        /**
         * Creates an AspectValue with validation.
         */
        fun create(value: String): Either<AspectValueError, AspectValue> = when {
            value.isBlank() -> AspectValueError.EmptyValue.left()
            value.length > MAX_LENGTH -> AspectValueError.TooLong(value, MAX_LENGTH).left()
            else -> AspectValue(value).right()
        }
    }

    /**
     * Check if this value represents a numeric value.
     */
    fun isNumeric(): Boolean = value.toDoubleOrNull() != null

    /**
     * Check if this value represents a boolean value.
     */
    fun isBoolean(): Boolean = value.lowercase() in setOf("true", "false", "yes", "no", "1", "0")

    /**
     * Convert to numeric value if possible.
     */
    fun toNumericValue(): Double? = value.toDoubleOrNull()

    /**
     * Convert to boolean value if possible.
     */
    fun toBooleanValue(): Boolean? = when (value.lowercase()) {
        "true", "yes", "1" -> true
        "false", "no", "0" -> false
        else -> null
    }

    /**
     * Check if this value represents an ISO 8601 duration.
     * Examples: "P1D" (1 day), "PT2H30M" (2 hours 30 minutes), "P1W" (1 week)
     */
    fun isDuration(): Boolean = try {
        parseDuration()
        true
    } catch (e: Exception) {
        false
    }

    /**
     * Parse ISO 8601 duration to Kotlin Duration.
     * Supports formats like:
     * - P1D (1 day)
     * - PT2H30M (2 hours 30 minutes)
     * - P1W (1 week)
     * - P2DT3H4M (2 days, 3 hours, 4 minutes)
     */
    fun parseDuration(): Duration? = try {
        parseISO8601Duration(value)
    } catch (e: Exception) {
        null
    }

    /**
     * Parse ISO 8601 duration string to Kotlin Duration.
     */
    private fun parseISO8601Duration(iso8601: String): Duration {
        require(iso8601.startsWith("P")) { "ISO 8601 duration must start with 'P'" }

        var remaining = iso8601.substring(1)
        var totalSeconds = 0L
        var inTimeSection = false

        val regex = Regex("(\\d+)([WDTHMS])")

        for (match in regex.findAll(remaining)) {
            val amount = match.groupValues[1].toLong()
            val unit = match.groupValues[2]

            when (unit) {
                "W" -> totalSeconds += amount * 7 * 24 * 60 * 60
                "D" -> totalSeconds += amount * 24 * 60 * 60
                "T" -> inTimeSection = true
                "H" -> {
                    require(inTimeSection) { "Hours must come after 'T' in ISO 8601 duration" }
                    totalSeconds += amount * 60 * 60
                }
                "M" -> {
                    if (inTimeSection) {
                        totalSeconds += amount * 60
                    } else {
                        // Months are not supported for simplicity
                        throw IllegalArgumentException("Month durations are not supported")
                    }
                }
                "S" -> {
                    require(inTimeSection) { "Seconds must come after 'T' in ISO 8601 duration" }
                    totalSeconds += amount
                }
            }
        }

        return totalSeconds.seconds
    }

    override fun toString(): String = value
}
