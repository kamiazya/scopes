package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Aspect validation specific errors.
 * These errors represent validation failures for aspect-related value objects.
 */
sealed class AspectValidationError : ScopesError() {
    // AspectKey validation errors
    data class EmptyAspectKey(override val occurredAt: Instant) : AspectValidationError()
    data class AspectKeyTooShort(override val occurredAt: Instant) : AspectValidationError()
    data class AspectKeyTooLong(val maxLength: Int, val actualLength: Int, override val occurredAt: Instant) : AspectValidationError()
    data class InvalidAspectKeyFormat(override val occurredAt: Instant) : AspectValidationError()

    // AspectValue validation errors
    data class EmptyAspectValue(override val occurredAt: Instant) : AspectValidationError()
    data class AspectValueTooShort(override val occurredAt: Instant) : AspectValidationError()
    data class AspectValueTooLong(val maxLength: Int, val actualLength: Int, override val occurredAt: Instant) : AspectValidationError()

    // AspectDefinition validation errors
    data class EmptyAspectAllowedValues(override val occurredAt: Instant) : AspectValidationError()
    data class DuplicateAspectAllowedValues(override val occurredAt: Instant) : AspectValidationError()
}
