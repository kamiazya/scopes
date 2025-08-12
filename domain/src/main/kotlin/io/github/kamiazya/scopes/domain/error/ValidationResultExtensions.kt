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


// Convenience constructors

/**
 * Creates a successful ValidationResult.
 */
fun <T> T.validationSuccess(): ValidationResult<T> = ValidationResult.Success(this)

/**
 * Creates a failed ValidationResult with a single error.
 */
fun DomainError.validationFailure(): ValidationResult<Nothing> =
    ValidationResult.Failure(nonEmptyListOf(this))

/**
 * Creates a failed ValidationResult with multiple errors.
 */
fun NonEmptyList<DomainError>.validationFailure(): ValidationResult<Nothing> =
    ValidationResult.Failure(this)




/**
 * Accumulates validation results for multiple values into a list.
 */
fun <T> List<ValidationResult<T>>.sequence(): ValidationResult<List<T>> {
    return ValidationResult.allSuccessOrAccumulateErrors(this).fold(
        { errors -> errors.validationFailure() },
        { values -> values.validationSuccess() }
    )
}


