package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValueError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
     * Note: This checks format validity, not whether the duration would be non-zero after truncation.
     */
    fun isDuration(): Boolean = try {
        parseISO8601DurationFormat(value)
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
     * Validate ISO 8601 duration format without full parsing.
     * Used by isDuration() to check format validity independently of truncation logic.
     */
    private fun parseISO8601DurationFormat(iso8601: String) {
        if (!iso8601.startsWith("P")) error("ISO 8601 duration must start with 'P'")
        if (iso8601.length <= 1) error("ISO 8601 duration must contain at least one component")

        // Handle week format (PnW must be alone, no other components allowed)
        val weekMatch = Regex("^P(\\d+)W$").matchEntire(iso8601)
        if (weekMatch != null) {
            val weeks = weekMatch.groupValues[1].toLong()
            if (weeks <= 0) error("ISO 8601 duration must specify at least one non-zero component")
            return // Valid week format
        }

        // Check for invalid week combinations
        if (iso8601.contains("W")) {
            error("Week durations cannot be combined with other components")
        }

        // Validate no negative values
        if (iso8601.contains("-")) {
            error("Negative durations are not supported")
        }

        // Split into date and time parts
        val tIndex = iso8601.indexOf('T')
        val datePart: String
        val timePart: String

        if (tIndex != -1) {
            datePart = iso8601.substring(1, tIndex)
            timePart = iso8601.substring(tIndex + 1)

            // Validate T is not at the end
            if (timePart.isEmpty()) {
                error("T separator must be followed by time components")
            }
        } else {
            datePart = iso8601.substring(1)
            timePart = ""

            // Check if time components appear in date part (invalid)
            if (datePart.contains(Regex("[HMS]"))) {
                error("Time components (H, M, S) must appear after T separator")
            }
        }

        // Parse date part (before T)
        if (datePart.isNotEmpty()) {
            // Validate date part structure and order
            val datePattern = Regex("^((\\d+)Y)?((\\d+)M)?((\\d+)D)?$")
            val dateMatch = datePattern.matchEntire(datePart)

            if (dateMatch == null) {
                error("Invalid date part format: $datePart")
            }

            val years = dateMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toLong()
            val months = dateMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toLong()

            if (years != null) error("Year durations are not supported")
            if (months != null) error("Month durations are not supported")
        }

        // Parse time part (after T)
        if (timePart.isNotEmpty()) {
            // Validate time part structure and order
            val timePattern = Regex("^((\\d+(?:\\.\\d+)?)H)?((\\d+(?:\\.\\d+)?)M)?((\\d+(?:\\.\\d+)?)S)?$")
            val timeMatch = timePattern.matchEntire(timePart)

            if (timeMatch == null) {
                error("Invalid time part format: $timePart")
            }
        }

        // Must have at least one non-zero component
        var hasNonZeroComponent = false

        // Check date components for non-zero values
        if (datePart.isNotEmpty()) {
            val datePattern = Regex("^(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?$")
            val dateMatch = datePattern.matchEntire(datePart)

            if (dateMatch != null) {
                val years = dateMatch.groupValues[1].takeIf { it.isNotEmpty() }?.toLong() ?: 0
                val months = dateMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toLong() ?: 0
                val days = dateMatch.groupValues[3].takeIf { it.isNotEmpty() }?.toLong() ?: 0

                if (days > 0) hasNonZeroComponent = true
            }
        }

        // Check time components for non-zero values
        if (timePart.isNotEmpty()) {
            val timePattern = Regex("^(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:(\\d+(?:\\.\\d+)?)S)?$")
            val timeMatch = timePattern.matchEntire(timePart)

            if (timeMatch != null) {
                val hours = timeMatch.groupValues[1].takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0
                val minutes = timeMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0
                val seconds = timeMatch.groupValues[3].takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0

                if (hours > 0 || minutes > 0 || seconds > 0) hasNonZeroComponent = true
            }
        }

        if (!hasNonZeroComponent) {
            error("ISO 8601 duration must specify at least one non-zero component")
        }
    }

    /**
     * Parse ISO 8601 duration string to Kotlin Duration.
     *
     * Note: Duration precision is intentionally limited to milliseconds
     * to ensure compatibility with database storage systems.
     * Nanosecond precision from fractional seconds will be truncated.
     */
    private fun parseISO8601Duration(iso8601: String): Duration {
        if (!iso8601.startsWith("P")) error("ISO 8601 duration must start with 'P'")
        if (iso8601.length <= 1) error("ISO 8601 duration must contain at least one component")

        // Handle week format (PnW must be alone, no other components allowed)
        val weekMatch = Regex("^P(\\d+)W$").matchEntire(iso8601)
        if (weekMatch != null) {
            val weeks = weekMatch.groupValues[1].toLong()
            return (weeks * 7 * 24 * 60 * 60).seconds
        }

        // Check for invalid week combinations
        if (iso8601.contains("W")) {
            error("Week durations cannot be combined with other components")
        }

        // Validate no negative values
        if (iso8601.contains("-")) {
            error("Negative durations are not supported")
        }

        // Split into date and time parts
        val tIndex = iso8601.indexOf('T')
        val datePart: String
        val timePart: String

        if (tIndex != -1) {
            datePart = iso8601.substring(1, tIndex)
            timePart = iso8601.substring(tIndex + 1)

            // Validate T is not at the end
            if (timePart.isEmpty()) {
                error("T separator must be followed by time components")
            }
        } else {
            datePart = iso8601.substring(1)
            timePart = ""

            // Check if time components appear in date part (invalid)
            if (datePart.contains(Regex("[HMS]"))) {
                error("Time components (H, M, S) must appear after T separator")
            }
        }

        var totalSeconds = 0.0

        // Parse date part (before T)
        if (datePart.isNotEmpty()) {
            // Validate date part structure and order
            val datePattern = Regex("^((\\d+)Y)?((\\d+)M)?((\\d+)D)?$")
            val dateMatch = datePattern.matchEntire(datePart)

            if (dateMatch == null) {
                error("Invalid date part format: $datePart")
            }

            val years = dateMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toLong()
            val months = dateMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toLong()
            val days = dateMatch.groupValues[6].takeIf { it.isNotEmpty() }?.toLong()

            if (years != null) error("Year durations are not supported")
            if (months != null) error("Month durations are not supported")
            if (days != null) {
                totalSeconds += days * 24 * 60 * 60
            }
        }

        // Parse time part (after T)
        if (timePart.isNotEmpty()) {
            // Validate time part structure and order
            val timePattern = Regex("^((\\d+(?:\\.\\d+)?)H)?((\\d+(?:\\.\\d+)?)M)?((\\d+(?:\\.\\d+)?)S)?$")
            val timeMatch = timePattern.matchEntire(timePart)

            if (timeMatch == null) {
                error("Invalid time part format: $timePart")
            }

            val hours = timeMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toDouble()
            val minutes = timeMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toDouble()
            val seconds = timeMatch.groupValues[6].takeIf { it.isNotEmpty() }?.toDouble()

            if (hours != null) {
                totalSeconds += hours * 60 * 60
            }
            if (minutes != null) {
                totalSeconds += minutes * 60
            }
            if (seconds != null) {
                totalSeconds += seconds
            }
        }

        // Convert to milliseconds to preserve fractional seconds up to millisecond precision
        // Intentionally truncate sub-millisecond precision for database compatibility
        val milliseconds = (totalSeconds * 1000).toLong()

        // Check if we have a non-zero duration after truncation
        if (milliseconds <= 0) {
            error("ISO 8601 duration must specify at least one non-zero component")
        }

        return milliseconds.milliseconds
    }

    override fun toString(): String = value
}
