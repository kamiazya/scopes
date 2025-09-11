package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Aspect validation specific errors.
 * These errors represent validation failures for aspect-related value objects.
 */
sealed class AspectValidationError : ScopesError() {
    // AspectKey validation errors
    data class EmptyAspectKey() : AspectValidationError()
    data class AspectKeyTooShort() : AspectValidationError()
    data class AspectKeyTooLong(val maxLength: Int, val actualLength: Int) : AspectValidationError()
    data class InvalidAspectKeyFormat() : AspectValidationError()

    // AspectValue validation errors
    data class EmptyAspectValue() : AspectValidationError()
    data class AspectValueTooShort() : AspectValidationError()
    data class AspectValueTooLong(val maxLength: Int, val actualLength: Int) : AspectValidationError()

    // AspectDefinition validation errors
    data class EmptyAspectAllowedValues() : AspectValidationError()
    data class DuplicateAspectAllowedValues() : AspectValidationError()
}
