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
        require(iso8601.length > 1) { "ISO 8601 duration must contain at least one component" }

        // Split into date and time parts
        val parts = iso8601.substring(1).split("T", limit = 2)
        val datePart = parts[0]
        val timePart = parts.getOrNull(1) ?: ""

        var totalSeconds = 0L

        // Parse date part (before T)
        if (datePart.isNotEmpty()) {
            val dateRegex = Regex("(\\d+)([YMD])")
            var lastUnit = '@' // Use @ as a marker that's lexicographically before all valid units

            for (match in dateRegex.findAll(datePart)) {
                val amount = match.groupValues[1].toLong()
                val unit = match.groupValues[2][0]

                // Validate order: Y must come before M, M before D
                require(unit > lastUnit) { "Invalid ISO 8601 duration: $unit must come after $lastUnit" }
                lastUnit = unit

                when (unit) {
                    'Y' -> throw IllegalArgumentException("Year durations are not supported")
                    'M' -> throw IllegalArgumentException("Month durations are not supported")
                    'D' -> totalSeconds += amount * 24 * 60 * 60
                }
            }
        }

        // Handle week format (PnW must be alone, no other components allowed)
        val weekMatch = Regex("^(\\d+)W$").matchEntire(iso8601.substring(1))
        if (weekMatch != null) {
            require(datePart.isEmpty() && timePart.isEmpty()) { "Week duration cannot be combined with other components" }
            val weeks = weekMatch.groupValues[1].toLong()
            return (weeks * 7 * 24 * 60 * 60).seconds
        }

        // Parse time part (after T)
        if (timePart.isNotEmpty()) {
            val timeRegex = Regex("(\\d+(?:\\.\\d+)?)([HMS])")
            var lastUnit = '@'

            for (match in timeRegex.findAll(timePart)) {
                val amount = match.groupValues[1].toDouble()
                val unit = match.groupValues[2][0]

                // Validate order: H must come before M, M before S
                require(unit > lastUnit) { "Invalid ISO 8601 duration: $unit must come after $lastUnit in time part" }
                lastUnit = unit

                when (unit) {
                    'H' -> totalSeconds += (amount * 60 * 60).toLong()
                    'M' -> totalSeconds += (amount * 60).toLong()
                    'S' -> totalSeconds += amount.toLong()
                }
            }
        }

        require(totalSeconds > 0) { "ISO 8601 duration must specify at least one non-zero component" }
        return totalSeconds.seconds
    }

    override fun toString(): String = value
}
