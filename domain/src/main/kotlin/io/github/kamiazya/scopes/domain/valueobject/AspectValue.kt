package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.AspectValidationError

/**
 * Value object representing an aspect value with embedded validation.
 * Aspect values can be text, numbers, or ordered values for different classification types.
 * Examples: "high", "medium", "low" for priority; "backend", "frontend" for type
 */
@JvmInline
value class AspectValue private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 100
        const val MIN_LENGTH = 1

        /**
         * Create a validated AspectValue from a string.
         * Returns Either with validation error or valid AspectValue.
         */
        fun create(value: String): Either<AspectValidationError, AspectValue> = either {
            val trimmedValue = value.trim()

            ensure(trimmedValue.isNotBlank()) { AspectValidationError.EmptyAspectValue }
            ensure(trimmedValue.length >= MIN_LENGTH) { AspectValidationError.AspectValueTooShort }
            ensure(trimmedValue.length <= MAX_LENGTH) {
                AspectValidationError.AspectValueTooLong(MAX_LENGTH, trimmedValue.length)
            }

            AspectValue(trimmedValue)
        }
    }

    /**
     * Check if this value can be compared as a number.
     */
    fun isNumeric(): Boolean = value.toDoubleOrNull() != null

    /**
     * Get numeric value if this AspectValue represents a number.
     */
    fun toNumericValue(): Double? = value.toDoubleOrNull()

    /**
     * Check if this value can be interpreted as a boolean.
     */
    fun isBoolean(): Boolean = value.lowercase() in listOf("true", "false")

    /**
     * Get boolean value if this AspectValue represents a boolean.
     */
    fun toBooleanValue(): kotlin.Boolean? = when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }

    override fun toString(): String = value
}
