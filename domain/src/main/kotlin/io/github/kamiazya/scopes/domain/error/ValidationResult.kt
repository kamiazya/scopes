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
     * Using Nothing as the type parameter ensures type safety and proper variance.
     */
    data class Failure(val errors: NonEmptyList<DomainError>) : ValidationResult<Nothing>()


    companion object {

        /**
         * Returns all successful values if all results are successful,
         * or accumulates all errors if any result fails.
         */
        fun <T> allSuccessOrAccumulateErrors(
            results: List<ValidationResult<T>>
        ): Either<NonEmptyList<DomainError>, List<T>> {
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
