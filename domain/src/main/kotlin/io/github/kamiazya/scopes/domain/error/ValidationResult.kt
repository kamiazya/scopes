package io.github.kamiazya.scopes.domain.error

import arrow.core.Either
import arrow.core.NonEmptyList

/**
 * Represents the result of a validation operation that can accumulate multiple errors.
 * A key component for error accumulation support.
 */
sealed class ValidationResult<out T> {

    /**
     * Represents a successful validation containing a valid value.
     */
    data class Success<T>(val value: T) : ValidationResult<T>()

    /**
     * Represents a failed validation containing accumulated errors.
     */
    data class Failure<T>(val errors: NonEmptyList<DomainError>) : ValidationResult<T>()

    /**
     * Combines this ValidationResult with another, applying the given function if both are successful,
     * or accumulating errors if either fails.
     */
    fun <U, V> combine(other: ValidationResult<U>, f: (T, U) -> V): ValidationResult<V> {
        return when (this) {
            is Success -> when (other) {
                is Success -> Success(f(this.value, other.value))
                is Failure -> Failure(other.errors)
            }
            is Failure -> when (other) {
                is Success -> Failure(this.errors)
                is Failure -> Failure(this.errors + other.errors)
            }
        }
    }

    /**
     * Converts this ValidationResult to an Either.
     */
    fun toEither(): Either<NonEmptyList<DomainError>, T> {
        return when (this) {
            is Success -> Either.Right(value)
            is Failure -> Either.Left(errors)
        }
    }

    companion object {
        /**
         * Creates a ValidationResult from an Either.
         */
        fun <T> fromEither(either: Either<NonEmptyList<DomainError>, T>): ValidationResult<T> {
            return either.fold(
                { errors -> Failure(errors) },
                { value -> Success(value) }
            )
        }

        /**
         * Collects all validation errors from a list of ValidationResults.
         */
        fun <T> collectValidationErrors(results: List<ValidationResult<T>>): List<DomainError> {
            return results.filterIsInstance<Failure<T>>()
                .flatMap { it.errors }
        }

        /**
         * Returns all successful values if all results are successful,
         * or accumulates all errors if any result fails.
         */
        fun <T> allSuccessOrAccumulateErrors(results: List<ValidationResult<T>>): Either<NonEmptyList<DomainError>, List<T>> {
            val successes = mutableListOf<T>()
            val errors = mutableListOf<DomainError>()

            results.forEach { result ->
                when (result) {
                    is Success -> successes.add(result.value)
                    is Failure -> errors.addAll(result.errors)
                }
            }

            return if (errors.isEmpty()) {
                Either.Right(successes)
            } else {
                Either.Left(NonEmptyList(errors.first(), errors.drop(1)))
            }
        }
    }
}
