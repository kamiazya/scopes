package io.github.kamiazya.scopes.platform.commons.validation

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf

/**
 * Simple validation result for accumulating errors.
 *
 * This is a lightweight alternative to Either when you need to accumulate multiple validation errors.
 */
sealed class ValidationResult<out T> {
    data class Success<T>(val value: T) : ValidationResult<T>()
    data class Failure(val errors: NonEmptyList<Any>) : ValidationResult<Nothing>()

    fun <R> map(transform: (T) -> R): ValidationResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}

/**
 * Extension functions for ValidationResult.
 */
fun <T> T.validationSuccess(): ValidationResult<T> = ValidationResult.Success(this)

fun Any.validationFailure(): ValidationResult.Failure = ValidationResult.Failure(nonEmptyListOf(this))

fun <T> combineValidations(validations: List<ValidationResult<T>>): ValidationResult<List<T>> {
    val errors = mutableListOf<Any>()
    val successes = mutableListOf<T>()

    validations.forEach { validation ->
        when (validation) {
            is ValidationResult.Failure -> errors.addAll(validation.errors)
            is ValidationResult.Success -> successes.add(validation.value)
        }
    }

    return if (errors.isEmpty()) {
        ValidationResult.Success(successes)
    } else {
        ValidationResult.Failure(NonEmptyList(errors.first(), errors.drop(1)))
    }
}
