package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValueError

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

    override fun toString(): String = value
}
