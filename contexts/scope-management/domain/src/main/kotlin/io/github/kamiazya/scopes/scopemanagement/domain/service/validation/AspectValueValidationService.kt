package io.github.kamiazya.scopes.scopemanagement.domain.service.validation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Domain service for validating aspect values against their definitions.
 * Contains the business logic for type-specific validation rules.
 */
class AspectValueValidationService {

    /**
     * Validate a value against the provided aspect definition.
     * @param definition The aspect definition containing validation rules
     * @param value The aspect value to validate
     * @return Either an error or the validated AspectValue
     */
    fun validateValue(definition: AspectDefinition, value: AspectValue): Either<ScopesError, AspectValue> {
        // Type-specific validation
        return when (definition.type) {
            is AspectType.Text -> {
                // Text type accepts any string value
                value.right()
            }
            is AspectType.Numeric -> {
                if (!value.isNumeric()) {
                    ScopesError.ValidationFailed(
                        "Value '${value.value}' is not a valid number for aspect '${definition.key.value}'",
                        ValidationError.InvalidNumericValue(definition.key.value, value.value),
                    ).left()
                } else {
                    value.right()
                }
            }
            is AspectType.BooleanType -> {
                if (!value.isBoolean()) {
                    ScopesError.ValidationFailed(
                        "Value '${value.value}' is not a valid boolean for aspect '${definition.key.value}'",
                        ValidationError.InvalidBooleanValue(definition.key.value, value.value),
                    ).left()
                } else {
                    value.right()
                }
            }
            is AspectType.Ordered -> {
                val orderedType = definition.type as AspectType.Ordered
                if (!orderedType.allowedValues.contains(value)) {
                    ScopesError.ValidationFailed(
                        "Value '${value.value}' is not allowed for aspect '${definition.key.value}'. Allowed values: ${orderedType.allowedValues.map {
                            it.value
                        }.joinToString(", ")}",
                        ValidationError.ValueNotInAllowedList(
                            definition.key.value,
                            value.value,
                            orderedType.allowedValues.map { it.value },
                        ),
                    ).left()
                } else {
                    value.right()
                }
            }
            is AspectType.Duration -> {
                if (!value.isDuration()) {
                    ScopesError.ValidationFailed(
                        "Value '${value.value}' is not a valid ISO 8601 duration for aspect '${definition.key.value}' (e.g., 'P1D', 'PT2H30M')",
                        ValidationError.InvalidDurationValue(definition.key.value, value.value),
                    ).left()
                } else {
                    value.right()
                }
            }
        }
    }

    /**
     * Validate that multiple values are allowed for an aspect definition.
     * @param definition The aspect definition
     * @param valueCount Number of values being validated
     * @return Either an error or Unit if multiple values are allowed
     */
    fun validateMultipleValuesAllowed(definition: AspectDefinition, valueCount: Int): Either<ScopesError, Unit> =
        if (valueCount > 1 && !definition.allowMultiple) {
            ScopesError.ValidationFailed(
                "Multiple values are not allowed for aspect '${definition.key.value}'",
                ValidationError.MultipleValuesNotAllowed(definition.key.value),
            ).left()
        } else {
            Unit.right()
        }

    /**
     * Validate required aspects are present.
     * @param providedKeys Set of provided aspect keys
     * @param requiredKeys Set of required aspect keys
     * @return Either an error or Unit if all required aspects are present
     */
    fun validateRequiredAspects(providedKeys: Set<String>, requiredKeys: Set<String>): Either<ScopesError, Unit> {
        val missingKeys = requiredKeys - providedKeys
        return if (missingKeys.isNotEmpty()) {
            ScopesError.ValidationFailed(
                "Required aspects are missing: ${missingKeys.joinToString(", ")}",
                ValidationError.RequiredAspectsMissing(missingKeys),
            ).left()
        } else {
            Unit.right()
        }
    }
}
