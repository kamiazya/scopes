package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Validation-specific errors.
 */
sealed class ValidationError {
    data class InvalidNumericValue(val aspectKey: String, val value: String) : ValidationError()

    data class InvalidBooleanValue(val aspectKey: String, val value: String) : ValidationError()

    data class ValueNotInAllowedList(val aspectKey: String, val value: String, val allowedValues: List<String>) : ValidationError()

    data class MultipleValuesNotAllowed(val aspectKey: String) : ValidationError()

    data class RequiredAspectsMissing(val missingKeys: Set<String>) : ValidationError()

    data class InvalidDurationValue(val aspectKey: String, val value: String) : ValidationError()
}
