package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import kotlinx.datetime.Instant

/**
 * Validation-specific errors.
 */
sealed class ValidationError : ScopesError() {
    data class InvalidNumericValue(val aspectKey: AspectKey, val value: AspectValue, override val occurredAt: Instant) : ValidationError()

    data class InvalidBooleanValue(val aspectKey: AspectKey, val value: AspectValue, override val occurredAt: Instant) : ValidationError()

    data class ValueNotInAllowedList(val aspectKey: AspectKey, val value: AspectValue, val allowedValues: List<AspectValue>, override val occurredAt: Instant) :
        ValidationError()

    data class MultipleValuesNotAllowed(val aspectKey: AspectKey, override val occurredAt: Instant) : ValidationError()

    data class RequiredAspectsMissing(val missingKeys: Set<AspectKey>, override val occurredAt: Instant) : ValidationError()

    data class InvalidDurationValue(val aspectKey: AspectKey, val value: AspectValue, override val occurredAt: Instant) : ValidationError()
}
