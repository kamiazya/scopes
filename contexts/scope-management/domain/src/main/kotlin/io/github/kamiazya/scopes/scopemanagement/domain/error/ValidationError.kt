package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Validation-specific errors.
 */
sealed class ValidationError : ScopesError() {
    data class InvalidNumericValue(val aspectKey: AspectKey, val value: AspectValue) : ValidationError()

    data class InvalidBooleanValue(val aspectKey: AspectKey, val value: AspectValue) : ValidationError()

    data class ValueNotInAllowedList(val aspectKey: AspectKey, val value: AspectValue, val allowedValues: List<AspectValue>) : ValidationError()

    data class MultipleValuesNotAllowed(val aspectKey: AspectKey) : ValidationError()

    data class RequiredAspectsMissing(val missingKeys: Set<AspectKey>) : ValidationError()

    data class InvalidDurationValue(val aspectKey: AspectKey, val value: AspectValue) : ValidationError()
}
