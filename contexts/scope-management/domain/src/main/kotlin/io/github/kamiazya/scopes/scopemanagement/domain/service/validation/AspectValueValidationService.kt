package io.github.kamiazya.scopes.scopemanagement.domain.service.validation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
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
                        field = definition.key.value,
                        value = value.value,
                        constraint = ScopesError.ValidationConstraintType.InvalidType(
                            expectedType = "number",
                            actualType = "text",
                        ),
                        details = mapOf(
                            "error" to ValidationError.InvalidNumericValue(definition.key, value, occurredAt = kotlinx.datetime.Clock.System.now()),
                        ),
                    ).left()
                } else {
                    value.right()
                }
            }
            is AspectType.BooleanType -> {
                if (!value.isBoolean()) {
                    ScopesError.ValidationFailed(
                        field = definition.key.value,
                        value = value.value,
                        constraint = ScopesError.ValidationConstraintType.InvalidType(
                            expectedType = "boolean",
                            actualType = "text",
                        ),
                        details = mapOf(
                            "error" to ValidationError.InvalidBooleanValue(definition.key, value, occurredAt = kotlinx.datetime.Clock.System.now()),
                        ),
                    ).left()
                } else {
                    value.right()
                }
            }
            is AspectType.Ordered -> {
                val orderedType = definition.type as AspectType.Ordered
                if (!orderedType.allowedValues.contains(value)) {
                    ScopesError.ValidationFailed(
                        field = definition.key.value,
                        value = value.value,
                        constraint = ScopesError.ValidationConstraintType.NotInAllowedValues(
                            allowedValues = orderedType.allowedValues.map { it.value },
                        ),
                        details = mapOf(
                            "error" to ValidationError.ValueNotInAllowedList(
                                definition.key,
                                value,
                                orderedType.allowedValues,
                                occurredAt = kotlinx.datetime.Clock.System.now(),
                            ),
                        ),
                    ).left()
                } else {
                    value.right()
                }
            }
            is AspectType.Duration -> {
                if (!value.isDuration()) {
                    ScopesError.ValidationFailed(
                        field = definition.key.value,
                        value = value.value,
                        constraint = ScopesError.ValidationConstraintType.InvalidFormat(
                            expectedFormat = "ISO 8601 duration (e.g., 'P1D', 'PT2H30M')",
                        ),
                        details = mapOf(
                            "error" to ValidationError.InvalidDurationValue(definition.key, value, occurredAt = kotlinx.datetime.Clock.System.now()),
                        ),
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
                field = definition.key.value,
                value = valueCount.toString(),
                constraint = ScopesError.ValidationConstraintType.MultipleValuesNotAllowed(
                    field = definition.key.value,
                ),
                details = mapOf("error" to ValidationError.MultipleValuesNotAllowed(definition.key, occurredAt = kotlinx.datetime.Clock.System.now())),
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
            // Convert strings to AspectKey objects for the error
            // We can safely assume these are valid since they come from the system
            val missingAspectKeys = missingKeys.mapNotNull { key ->
                AspectKey.create(key).getOrNull()
            }.toSet()

            ScopesError.ValidationFailed(
                field = "aspects",
                value = providedKeys.joinToString(", "),
                constraint = ScopesError.ValidationConstraintType.MissingRequired(
                    requiredFields = missingKeys.toList(),
                ),
                details = mapOf("error" to ValidationError.RequiredAspectsMissing(missingAspectKeys, occurredAt = kotlinx.datetime.Clock.System.now())),
            ).left()
        } else {
            Unit.right()
        }
    }
}
