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
            value.length > 1000 -> AspectValueError.TooLong(actualLength = value.length, maxLength = 1000).left()
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
        validateBasicFormat(iso8601)

        val weekDuration = parseWeekFormat(iso8601, validateOnly)
        if (weekDuration != null) return weekDuration

        validateNonWeekFormat(iso8601)

        val (datePart, timePart) = splitDateAndTimeParts(iso8601)
        val days = parseDatePart(datePart)
        val (hours, minutes, seconds) = parseTimePart(timePart)

        validateNonZeroComponents(days, hours, minutes, seconds)

        return if (validateOnly) {
            Duration.ZERO
        } else {
            calculateDuration(days, hours, minutes, seconds)
        }
    }

    /**
     * Validates the basic format requirements for ISO 8601 duration.
     */
    private fun validateBasicFormat(iso8601: String) {
        if (!iso8601.startsWith("P")) error("ISO 8601 duration must start with 'P'")
        if (iso8601.length <= 1) error("ISO 8601 duration must contain at least one component")
        if (iso8601.contains("-")) error("Negative durations are not supported")
    }

    /**
     * Attempts to parse week format (PnW). Returns Duration if successful, null otherwise.
     */
    private fun parseWeekFormat(iso8601: String, validateOnly: Boolean): Duration? {
        val weekMatch = WEEK_PATTERN.matchEntire(iso8601) ?: return null

        val weeks = weekMatch.groupValues[1].toLong()
        if (weeks <= 0) error("ISO 8601 duration must specify at least one non-zero component")

        return if (validateOnly) {
            Duration.ZERO
        } else {
            (weeks * 7 * 24 * 60 * 60).seconds
        }
    }

    /**
     * Validates that week format is not mixed with other components.
     */
    private fun validateNonWeekFormat(iso8601: String) {
        if (iso8601.contains("W")) {
            error("Week durations cannot be combined with other components")
        }
    }

    /**
     * Splits the duration string into date and time parts.
     */
    private fun splitDateAndTimeParts(iso8601: String): Pair<String, String> {
        val tIndex = iso8601.indexOf('T')

        return if (tIndex != -1) {
            val datePart = iso8601.substring(1, tIndex)
            val timePart = iso8601.substring(tIndex + 1)

            if (timePart.isEmpty()) {
                error("T separator must be followed by time components")
            }

            Pair(datePart, timePart)
        } else {
            val datePart = iso8601.substring(1)

            if (datePart.contains(Regex("[HMS]"))) {
                error("Time components (H, M, S) must appear after T separator")
            }

            Pair(datePart, "")
        }
    }

    /**
     * Parses the date part and returns days value.
     */
    private fun parseDatePart(datePart: String): Long {
        if (datePart.isEmpty()) return 0L

        val dateMatch = DATE_PATTERN.matchEntire(datePart)
            ?: error("Invalid date part format: $datePart")

        val years = dateMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toLong()
        val months = dateMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toLong()
        val days = dateMatch.groupValues[6].takeIf { it.isNotEmpty() }?.toLong()

        if (years != null) error("Year durations are not supported")
        if (months != null) error("Month durations are not supported")

        return days ?: 0L
    }

    /**
     * Parses the time part and returns hours, minutes, seconds.
     */
    private fun parseTimePart(timePart: String): Triple<Double, Double, Double> {
        if (timePart.isEmpty()) return Triple(0.0, 0.0, 0.0)

        val timeMatch = TIME_PATTERN.matchEntire(timePart)
            ?: error("Invalid time part format: $timePart")

        val hours = timeMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0
        val minutes = timeMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0
        val seconds = timeMatch.groupValues[6].takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0

        return Triple(hours, minutes, seconds)
    }

    /**
     * Validates that at least one component is non-zero.
     */
    private fun validateNonZeroComponents(days: Long, hours: Double, minutes: Double, seconds: Double) {
        if (days <= 0 && hours <= 0 && minutes <= 0 && seconds <= 0) {
            error("ISO 8601 duration must specify at least one non-zero component")
        }
    }

    /**
     * Calculates the final duration from parsed components.
     */
    private fun calculateDuration(days: Long, hours: Double, minutes: Double, seconds: Double): Duration {
        val totalSeconds = days * 24 * 60 * 60 + hours * 60 * 60 + minutes * 60 + seconds
        val milliseconds = (totalSeconds * 1000).toLong()

        if (milliseconds <= 0) {
            error("ISO 8601 duration must specify at least one non-zero component")
        }

        return milliseconds.milliseconds
    }

    override fun toString(): String = value
}
