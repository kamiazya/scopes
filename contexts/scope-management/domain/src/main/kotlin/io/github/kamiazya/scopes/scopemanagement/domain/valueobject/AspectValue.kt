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
        // Common regex patterns for ISO 8601 duration parsing
        private val WEEK_PATTERN = Regex("^P(\\d+)W$")
        private val DATE_PATTERN = Regex("^((\\d+)Y)?((\\d+)M)?((\\d+)D)?$")
        private val TIME_PATTERN = Regex("^((\\d+(?:\\.\\d+)?)H)?((\\d+(?:\\.\\d+)?)M)?((\\d+(?:\\.\\d+)?)S)?$")

        /**
         * Create an AspectValue with validation.
         * Returns Either.Left with error if validation fails.
         */
        fun create(value: String): Either<AspectValueError, AspectValue> = when {
            value.isBlank() -> AspectValueError.EmptyValue.left()
            value.length > 1000 -> AspectValueError.TooLong(value.length, 1000).left()
            else -> AspectValue(value).right()
        }

        /**
         * Create an AspectValue without validation (for internal use).
         * Use with caution - only when the value is known to be valid.
         */
        fun unsafeCreate(value: String): AspectValue = AspectValue(value)
    }

    /**
     * Check if this value represents a numeric value.
     */
    fun isNumeric(): Boolean = value.toDoubleOrNull() != null

    /**
     * Parse as numeric value if possible.
     * Returns null if not a valid number.
     */
    fun toNumericValue(): Double? = value.toDoubleOrNull()

    /**
     * Check if this value represents a boolean.
     * Recognizes: true/false, yes/no, 1/0 (case-insensitive)
     */
    fun isBoolean(): Boolean = parseBoolean() != null

    /**
     * Parse as boolean if possible.
     * Returns null if not a recognized boolean value.
     */
    fun parseBoolean(): Boolean? = when (value.lowercase()) {
        "true", "yes", "1" -> true
        "false", "no", "0" -> false
        else -> null
    }

    /**
     * Alias for parseBoolean() for backward compatibility.
     */
    fun toBooleanValue(): Boolean? = parseBoolean()

    /**
     * Compare with another AspectValue.
     * Used for numeric and boolean comparisons.
     */
    operator fun compareTo(other: AspectValue): Int {
        // Try numeric comparison first
        val thisNum = this.toNumericValue()
        val otherNum = other.toNumericValue()
        if (thisNum != null && otherNum != null) {
            return thisNum.compareTo(otherNum)
        }

        // Try boolean comparison
        val thisBool = this.toBooleanValue()
        val otherBool = other.toBooleanValue()
        if (thisBool != null && otherBool != null) {
            return thisBool.compareTo(otherBool)
        }

        // Fall back to string comparison
        return this.value.compareTo(other.value)
    }

    /**
     * Check if this value represents an ISO 8601 duration.
     * Examples: "P1D" (1 day), "PT2H30M" (2 hours 30 minutes), "P1W" (1 week)
     */
    fun isDuration(): Boolean = try {
        parseISO8601DurationInternal(value, validateOnly = true)
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
     *
     * Note: Duration precision is intentionally limited to milliseconds
     * to ensure compatibility with database storage systems.
     */
    fun parseDuration(): Duration? = try {
        parseISO8601DurationInternal(value, validateOnly = false)
    } catch (e: Exception) {
        null
    }

    /**
     * Internal method to parse ISO 8601 duration.
     *
     * @param iso8601 The ISO 8601 duration string
     * @param validateOnly If true, only validates format without computing duration
     * @return The parsed Duration, or throws an error if invalid
     */
    private fun parseISO8601DurationInternal(iso8601: String, validateOnly: Boolean): Duration {
        // Basic format validation
        if (!iso8601.startsWith("P")) error("ISO 8601 duration must start with 'P'")
        if (iso8601.length <= 1) error("ISO 8601 duration must contain at least one component")

        // Handle week format (PnW must be alone, no other components allowed)
        val weekMatch = WEEK_PATTERN.matchEntire(iso8601)
        if (weekMatch != null) {
            val weeks = weekMatch.groupValues[1].toLong()
            if (weeks <= 0) error("ISO 8601 duration must specify at least one non-zero component")
            return if (validateOnly) {
                Duration.ZERO // Just return a valid duration for validation
            } else {
                (weeks * 7 * 24 * 60 * 60).seconds
            }
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

        var days = 0L
        var hours = 0.0
        var minutes = 0.0
        var seconds = 0.0

        // Parse date part (before T)
        if (datePart.isNotEmpty()) {
            val dateMatch = DATE_PATTERN.matchEntire(datePart)
            if (dateMatch == null) {
                error("Invalid date part format: $datePart")
            }

            val years = dateMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toLong()
            val months = dateMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toLong()
            val daysValue = dateMatch.groupValues[6].takeIf { it.isNotEmpty() }?.toLong()

            if (years != null) error("Year durations are not supported")
            if (months != null) error("Month durations are not supported")
            if (daysValue != null) {
                days = daysValue
            }
        }

        // Parse time part (after T)
        if (timePart.isNotEmpty()) {
            val timeMatch = TIME_PATTERN.matchEntire(timePart)
            if (timeMatch == null) {
                error("Invalid time part format: $timePart")
            }

            val hoursValue = timeMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toDouble()
            val minutesValue = timeMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toDouble()
            val secondsValue = timeMatch.groupValues[6].takeIf { it.isNotEmpty() }?.toDouble()

            if (hoursValue != null) hours = hoursValue
            if (minutesValue != null) minutes = minutesValue
            if (secondsValue != null) seconds = secondsValue
        }

        // Check for at least one non-zero component
        if (days <= 0 && hours <= 0 && minutes <= 0 && seconds <= 0) {
            error("ISO 8601 duration must specify at least one non-zero component")
        }

        // If only validating, return a dummy duration
        if (validateOnly) {
            return Duration.ZERO
        }

        // Calculate total seconds
        val totalSeconds = days * 24 * 60 * 60 +
            hours * 60 * 60 +
            minutes * 60 +
            seconds

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
