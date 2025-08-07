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
 * Flat maps the success value if this is a Success, otherwise preserves the Failure.
 * Allows chaining validations that may fail.
 */
fun <T, U> ValidationResult<T>.flatMap(f: (T) -> ValidationResult<U>): ValidationResult<U> {
    return when (this) {
        is ValidationResult.Success -> f(value)
        is ValidationResult.Failure -> ValidationResult.Failure(errors)
    }
}

/**
 * Applies the given function to the success value, accumulating errors if both this and the result fail.
 * This is useful for validating multiple fields and collecting all errors.
 */
fun <T, U, V> ValidationResult<T>.ap(other: ValidationResult<(T) -> U>, f: (U, T) -> V): ValidationResult<V> {
    return when (this) {
        is ValidationResult.Success -> when (other) {
            is ValidationResult.Success -> ValidationResult.Success(f(other.value(this.value), this.value))
            is ValidationResult.Failure -> ValidationResult.Failure(other.errors)
        }
        is ValidationResult.Failure -> when (other) {
            is ValidationResult.Success -> ValidationResult.Failure(this.errors)
            is ValidationResult.Failure -> ValidationResult.Failure(this.errors + other.errors)
        }
    }
}

/**
 * Returns the success value if this is a Success, otherwise returns the default value.
 */
fun <T> ValidationResult<T>.getOrElse(default: T): T {
    return when (this) {
        is ValidationResult.Success -> value
        is ValidationResult.Failure -> default
    }
}

/**
 * Returns the success value if this is a Success, otherwise computes and returns the default value.
 */
fun <T> ValidationResult<T>.getOrElse(default: (NonEmptyList<DomainError>) -> T): T {
    return when (this) {
        is ValidationResult.Success -> value
        is ValidationResult.Failure -> default(errors)
    }
}

/**
 * Transforms errors to a different type while preserving success values.
 */
fun <T> ValidationResult<T>.mapErrors(f: (NonEmptyList<DomainError>) -> NonEmptyList<DomainError>): ValidationResult<T> {
    return when (this) {
        is ValidationResult.Success -> this
        is ValidationResult.Failure -> ValidationResult.Failure(f(errors))
    }
}

/**
 * Performs a side effect on the success value without changing the ValidationResult.
 */
fun <T> ValidationResult<T>.onSuccess(action: (T) -> Unit): ValidationResult<T> {
    if (this is ValidationResult.Success) {
        action(value)
    }
    return this
}

/**
 * Performs a side effect on the errors without changing the ValidationResult.
 */
fun <T> ValidationResult<T>.onFailure(action: (NonEmptyList<DomainError>) -> Unit): ValidationResult<T> {
    if (this is ValidationResult.Failure) {
        action(errors)
    }
    return this
}

/**
 * Returns true if this is a Success, false otherwise.
 */
val <T> ValidationResult<T>.isSuccess: Boolean
    get() = this is ValidationResult.Success

/**
 * Returns true if this is a Failure, false otherwise.
 */
val <T> ValidationResult<T>.isFailure: Boolean
    get() = this is ValidationResult.Failure

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
 * Validates a nullable value, creating a failure if null.
 */
fun <T> T?.validateNotNull(error: DomainError): ValidationResult<T> {
    return this?.validationSuccess() ?: error.validationFailure()
}

/**
 * Validates a condition, creating a success with the value if true, failure if false.
 */
fun <T> T.validateIf(condition: Boolean, error: DomainError): ValidationResult<T> {
    return if (condition) {
        validationSuccess()
    } else {
        error.validationFailure()
    }
}

/**
 * Validates a predicate, creating a success with the value if the predicate is true, failure if false.
 */
fun <T> T.validateWith(predicate: (T) -> Boolean, error: DomainError): ValidationResult<T> {
    return validateIf(predicate(this), error)
}

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
 * Maps and accumulates validation results.
 */
fun <T, U> List<T>.traverse(f: (T) -> ValidationResult<U>): ValidationResult<List<U>> {
    return map(f).sequence()
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
