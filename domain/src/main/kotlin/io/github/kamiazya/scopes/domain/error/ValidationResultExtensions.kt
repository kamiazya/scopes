package io.github.kamiazya.scopes.domain.error

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf

/**
 * Extensions for ValidationResult to provide ergonomic APIs for error accumulation.
 * These extensions enhance the basic ValidationResult with convenient methods following
 * Patterns for functional error handling.
 */

/**
 * Maps the success value if this is a Success, otherwise preserves the Failure.
 */
fun <T, U> ValidationResult<T>.map(f: (T) -> U): ValidationResult<U> {
    return when (this) {
        is ValidationResult.Success -> ValidationResult.Success(f(value))
        is ValidationResult.Failure -> ValidationResult.Failure(errors)
    }
}

/**
 * Folds the ValidationResult into a single value.
 */
fun <T, U> ValidationResult<T>.fold(
    ifFailure: (NonEmptyList<DomainError>) -> U,
    ifSuccess: (T) -> U
): U {
    return when (this) {
        is ValidationResult.Success -> ifSuccess(value)
        is ValidationResult.Failure -> ifFailure(errors)
    }
}

// Convenience constructors

/**
 * Creates a successful ValidationResult.
 */
fun <T> T.validationSuccess(): ValidationResult<T> = ValidationResult.Success(this)

/**
 * Creates a failed ValidationResult with a single error.
 */
fun <T> DomainError.validationFailure(): ValidationResult<T> =
    ValidationResult.Failure(nonEmptyListOf(this))

/**
 * Creates a failed ValidationResult with multiple errors.
 */
fun <T> NonEmptyList<DomainError>.validationFailure(): ValidationResult<T> =
    ValidationResult.Failure(this)




/**
 * Accumulates validation results for multiple values into a list.
 */
fun <T> List<ValidationResult<T>>.sequence(): ValidationResult<List<T>> {
    return ValidationResult.allSuccessOrAccumulateErrors(this).fold(
        { errors -> ValidationResult.Failure(errors) },
        { values -> ValidationResult.Success(values) }
    )
}


/**
 * Converts a ValidationResult with multiple errors to contain only the first error.
 * Useful for implementing fail-fast behavior when you have accumulated errors.
 */
fun <T> ValidationResult<T>.firstErrorOnly(): ValidationResult<T> {
    return when (this) {
        is ValidationResult.Success -> this
        is ValidationResult.Failure -> ValidationResult.Failure(nonEmptyListOf(errors.head))
    }
}
