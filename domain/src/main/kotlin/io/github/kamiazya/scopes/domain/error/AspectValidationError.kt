package io.github.kamiazya.scopes.domain.error

/**
 * Aspect validation specific errors.
 * These errors represent validation failures for aspect-related value objects.
 */
sealed class AspectValidationError {

    // AspectKey validation errors
    data object EmptyAspectKey : AspectValidationError()
    data object AspectKeyTooShort : AspectValidationError()
    data class AspectKeyTooLong(val maxLength: Int, val actualLength: Int) : AspectValidationError()
    data object InvalidAspectKeyFormat : AspectValidationError()

    // AspectValue validation errors
    data object EmptyAspectValue : AspectValidationError()
    data object AspectValueTooShort : AspectValidationError()
    data class AspectValueTooLong(val maxLength: Int, val actualLength: Int) : AspectValidationError()

    // AspectDefinition validation errors
    data object EmptyAspectAllowedValues : AspectValidationError()
    data object DuplicateAspectAllowedValues : AspectValidationError()
}
