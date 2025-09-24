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
 *
 * @param strictValidation Whether to enable strict validation mode
 * @param allowPartialMatches Whether to allow partial string matches in text validation
 */
class AspectValueValidationService(private val strictValidation: Boolean = true, private val allowPartialMatches: Boolean = false) {

    /**
     * Validate a value against the provided aspect definition.
     * @param definition The aspect definition containing validation rules
     * @param value The aspect value to validate
     * @return Either an error or the validated AspectValue
     */
    fun validateValue(definition: AspectDefinition, value: AspectValue): Either<ScopesError, AspectValue> {
        // Type-specific validation
        return when (definition.type) {
            is AspectType.Text -> validateTextValue(definition, value)
            is AspectType.Numeric -> validateNumericValue(definition, value)
            is AspectType.BooleanType -> validateBooleanValue(definition, value)
            is AspectType.Ordered -> validateOrderedValue(definition, value)
            is AspectType.Duration -> validateDurationValue(definition, value)
        }
    }

    private fun validateTextValue(definition: AspectDefinition, value: AspectValue): Either<ScopesError, AspectValue> {
        // Text type accepts any string value
        return value.right()
    }

    private fun validateNumericValue(definition: AspectDefinition, value: AspectValue): Either<ScopesError, AspectValue> = if (!value.isNumeric()) {
        createValidationError(
            definition,
            value,
            ScopesError.ValidationConstraintType.InvalidType(
                expectedType = "number",
                actualType = "text",
            ),
            ValidationError.InvalidNumericValue(definition.key, value),
        ).left()
    } else {
        value.right()
    }

    private fun validateBooleanValue(definition: AspectDefinition, value: AspectValue): Either<ScopesError, AspectValue> = if (!value.isBoolean()) {
        createValidationError(
            definition,
            value,
            ScopesError.ValidationConstraintType.InvalidType(
                expectedType = "boolean",
                actualType = "text",
            ),
            ValidationError.InvalidBooleanValue(definition.key, value),
        ).left()
    } else {
        value.right()
    }

    private fun validateOrderedValue(definition: AspectDefinition, value: AspectValue): Either<ScopesError, AspectValue> {
        val orderedType = definition.type as AspectType.Ordered
        return if (!orderedType.allowedValues.contains(value)) {
            createValidationError(
                definition,
                value,
                ScopesError.ValidationConstraintType.NotInAllowedValues(
                    allowedValues = orderedType.allowedValues.map { it.value },
                ),
                ValidationError.ValueNotInAllowedList(
                    definition.key,
                    value,
                    orderedType.allowedValues,
                ),
            ).left()
        } else {
            value.right()
        }
    }

    private fun validateDurationValue(definition: AspectDefinition, value: AspectValue): Either<ScopesError, AspectValue> = if (!value.isDuration()) {
        createValidationError(
            definition,
            value,
            ScopesError.ValidationConstraintType.InvalidFormat(
                expectedFormat = "ISO 8601 duration (e.g., 'P1D', 'PT2H30M')",
            ),
            ValidationError.InvalidDurationValue(definition.key, value),
        ).left()
    } else {
        value.right()
    }

    private fun createValidationError(
        definition: AspectDefinition,
        value: AspectValue,
        constraint: ScopesError.ValidationConstraintType,
        error: ValidationError,
    ): ScopesError.ValidationFailed = ScopesError.ValidationFailed(
        field = definition.key.value,
        value = value.value,
        constraint = constraint,
        details = mapOf("error" to error),
    )

    /**
     * Validate that multiple values are allowed for an aspect definition.
     * @param definition The aspect definition
     * @param valueCount Number of values being validated
     * @return Either an error or Unit if multiple values are allowed
     */
    fun validateMultipleValuesAllowed(definition: AspectDefinition, valueCount: Int): Either<ScopesError, Unit> = when {
        valueCount == 0 -> {
            ScopesError.ValidationFailed(
                field = definition.key.value,
                value = "empty",
                constraint = ScopesError.ValidationConstraintType.EmptyValues(
                    field = definition.key.value,
                ),
                details = mapOf("error" to ValidationError.EmptyValuesList(definition.key)),
            ).left()
        }
        valueCount > 1 && !definition.allowMultiple -> {
            ScopesError.ValidationFailed(
                field = definition.key.value,
                value = valueCount.toString(),
                constraint = ScopesError.ValidationConstraintType.MultipleValuesNotAllowed(
                    field = definition.key.value,
                ),
                details = mapOf("error" to ValidationError.MultipleValuesNotAllowed(definition.key)),
            ).left()
        }
        else -> Unit.right()
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
                details = mapOf("error" to ValidationError.RequiredAspectsMissing(missingAspectKeys)),
            ).left()
        } else {
            Unit.right()
        }
    }
}
