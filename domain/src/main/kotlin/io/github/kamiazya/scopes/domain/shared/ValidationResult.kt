package io.github.kamiazya.scopes.domain.shared

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.domain.error.ScopesError

/**
 * Simple validation result for accumulating errors.
 */
sealed class ValidationResult<out T> {
    data class Success<T>(val value: T) : ValidationResult<T>()
    data class Failure(val errors: NonEmptyList<ScopesError>) : ValidationResult<Nothing>()
    
    fun <R> map(transform: (T) -> R): ValidationResult<R> = when(this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}

/**
 * Extension functions for ValidationResult.
 */
fun <T> T.validationSuccess(): ValidationResult<T> = ValidationResult.Success(this)

fun ScopesError.validationFailure(): ValidationResult<Nothing> = 
    ValidationResult.Failure(nonEmptyListOf(this))

fun <T> combineValidations(vararg validations: ValidationResult<T>): ValidationResult<Unit> {
    val errors = mutableListOf<ScopesError>()
    
    validations.forEach { validation ->
        when (validation) {
            is ValidationResult.Failure -> errors.addAll(validation.errors)
            is ValidationResult.Success -> { /* continue */ }
        }
    }
    
    return if (errors.isEmpty()) {
        ValidationResult.Success(Unit)
    } else {
        ValidationResult.Failure(NonEmptyList(errors.first(), errors.drop(1)))
    }
}
