package io.github.kamiazya.scopes.scopemanagement.domain.entity

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.currentTimestamp
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Entity representing an aspect definition with type and constraints.
 * Defines the metadata about an aspect including its type and allowed values.
 * AspectDefinition is an entity because it has identity (AspectKey) and lifecycle.
 */
data class AspectDefinition private constructor(
    val key: AspectKey,
    val type: AspectType,
    val description: String? = null,
    val allowMultiple: Boolean = false,
) {
    companion object {
        /**
         * Create a text-based aspect definition.
         */
        fun createText(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition = AspectDefinition(
            key = key,
            type = AspectType.Text,
            description = description,
            allowMultiple = allowMultiple,
        )

        /**
         * Create a numeric aspect definition.
         */
        fun createNumeric(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition = AspectDefinition(
            key = key,
            type = AspectType.Numeric,
            description = description,
            allowMultiple = allowMultiple,
        )

        /**
         * Create a boolean aspect definition.
         */
        fun createBoolean(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition = AspectDefinition(
            key = key,
            type = AspectType.BooleanType,
            description = description,
            allowMultiple = allowMultiple,
        )

        /**
         * Create an ordered aspect definition with explicit value ordering.
         */
        fun createOrdered(
            key: AspectKey,
            allowedValues: List<AspectValue>,
            description: String? = null,
            allowMultiple: Boolean = false,
        ): Either<AspectValidationError, AspectDefinition> = either {
            ensure(allowedValues.isNotEmpty()) { AspectValidationError.EmptyAspectAllowedValues(occurredAt = currentTimestamp()) }
            ensure(allowedValues.size == allowedValues.distinct().size) {
                AspectValidationError.DuplicateAspectAllowedValues(occurredAt = currentTimestamp())
            }

            AspectDefinition(
                key = key,
                type = AspectType.Ordered(allowedValues),
                description = description,
                allowMultiple = allowMultiple,
            )
        }

        /**
         * Create a duration aspect definition.
         * Values must be in ISO 8601 duration format (e.g., "P1D", "PT2H30M").
         */
        fun createDuration(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition = AspectDefinition(
            key = key,
            type = AspectType.Duration,
            description = description,
            allowMultiple = allowMultiple,
        )
    }

    /**
     * Validate if a value is compatible with this aspect definition.
     */
    fun isValidValue(value: AspectValue): Boolean = when (type) {
        is AspectType.Text -> true
        is AspectType.Numeric -> value.isNumeric()
        is AspectType.BooleanType -> value.isBoolean()
        is AspectType.Ordered -> type.allowedValues.contains(value)
        is AspectType.Duration -> value.isDuration()
    }

    /**
     * Get the order index of a value for ordered types.
     * Returns null for non-ordered types or invalid values.
     */
    fun getValueOrder(value: AspectValue): Int? = when (type) {
        is AspectType.Ordered -> type.allowedValues.indexOf(value).takeIf { it >= 0 }
        else -> null
    }

    /**
     * Compare two values according to this aspect definition's type.
     * Returns negative if first < second, positive if first > second, 0 if equal.
     * Returns null if values cannot be compared with this definition.
     */
    fun compareValues(first: AspectValue, second: AspectValue): Int? = when (type) {
        is AspectType.Text -> first.value.compareTo(second.value)
        is AspectType.Numeric -> {
            val firstNum = first.toNumericValue()
            val secondNum = second.toNumericValue()
            if (firstNum != null && secondNum != null) {
                firstNum.compareTo(secondNum)
            } else {
                null
            }
        }
        is AspectType.BooleanType -> {
            val firstBool = first.toBooleanValue()
            val secondBool = second.toBooleanValue()
            if (firstBool != null && secondBool != null) {
                firstBool.compareTo(secondBool) // false < true
            } else {
                null
            }
        }
        is AspectType.Ordered -> {
            val firstOrder = getValueOrder(first)
            val secondOrder = getValueOrder(second)
            if (firstOrder != null && secondOrder != null) {
                firstOrder.compareTo(secondOrder)
            } else {
                null
            }
        }
        is AspectType.Duration -> {
            val firstDuration = first.parseDuration()
            val secondDuration = second.parseDuration()
            if (firstDuration != null && secondDuration != null) {
                firstDuration.compareTo(secondDuration)
            } else {
                null
            }
        }
    }
}
