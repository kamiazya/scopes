package io.github.kamiazya.scopes.domain.error

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf

/**
 * Bridge utilities for seamless integration between ValidationResult and Either types.
 *
 * Provides utility functions for converting between
 * ValidationResult and Either, enabling smooth integration in mixed codebases and
 * supporting async operations.
 */
object ValidationResultBridge {

    /**
     * Converts a list of Either values to a ValidationResult.
     * Accumulates all errors if any Either is Left, otherwise returns all Right values.
     *
     * @param eitherList List of Either values to convert
     * @return ValidationResult containing all successful values or accumulated errors
     */
    fun <T> sequenceToValidationResult(eitherList: List<Either<NonEmptyList<DomainError>, T>>): ValidationResult<List<T>> {
        val successes = mutableListOf<T>()
        val errors = mutableListOf<DomainError>()

        eitherList.forEach { either ->
            either.fold(
                { errorList -> errors.addAll(errorList) },
                { value -> successes.add(value) }
            )
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success(successes)
        } else {
            ValidationResult.Failure(NonEmptyList(errors.first(), errors.drop(1)))
        }
    }

    /**
     * Converts a list of ValidationResult values to an Either.
     * Accumulates all errors if any ValidationResult is Failure, otherwise returns all successful values.
     *
     * @param validationList List of ValidationResult values to convert
     * @return Either containing all successful values or accumulated errors
     */
    fun <T> sequenceToEither(validationList: List<ValidationResult<T>>): Either<NonEmptyList<DomainError>, List<T>> {
        val successes = mutableListOf<T>()
        val errors = mutableListOf<DomainError>()

        validationList.forEach { validation ->
            when (validation) {
                is ValidationResult.Success -> successes.add(validation.value)
                is ValidationResult.Failure -> errors.addAll(validation.errors)
            }
        }

        return if (errors.isEmpty()) {
            Either.Right(successes)
        } else {
            Either.Left(NonEmptyList(errors.first(), errors.drop(1)))
        }
    }

    /**
     * Recovers from ValidationResult failure using an Either-based recovery function.
     *
     * @param validation The ValidationResult to potentially recover from
     * @param recovery Function that takes errors and returns an Either for recovery
     * @return ValidationResult after applying recovery if needed
     */
    fun <T> recover(
        validation: ValidationResult<T>,
        recovery: (NonEmptyList<DomainError>) -> Either<NonEmptyList<DomainError>, T>
    ): ValidationResult<T> {
        return when (validation) {
            is ValidationResult.Success -> validation
            is ValidationResult.Failure -> {
                recovery(validation.errors).fold(
                    { newErrors -> ValidationResult.Failure(newErrors) },
                    { recoveredValue -> ValidationResult.Success(recoveredValue) }
                )
            }
        }
    }

    /**
     * Recovers from Either failure using a ValidationResult-based recovery function.
     *
     * @param either The Either to potentially recover from
     * @param recovery Function that takes errors and returns a ValidationResult for recovery
     * @return Either after applying recovery if needed
     */
    fun <T> recoverWithValidation(
        either: Either<NonEmptyList<DomainError>, T>,
        recovery: (NonEmptyList<DomainError>) -> ValidationResult<T>
    ): Either<NonEmptyList<DomainError>, T> {
        return either.fold(
            { errors ->
                recovery(errors).fold(
                    { newErrors -> Either.Left(newErrors) },
                    { recoveredValue -> Either.Right(recoveredValue) }
                )
            },
            { value -> Either.Right(value) }
        )
    }

    /**
     * Converts a single Either to ValidationResult, wrapping single errors in NonEmptyList.
     *
     * @param either Either value to convert
     * @return ValidationResult with error wrapped in NonEmptyList if Left
     */
    fun <T> toValidationResultNel(either: Either<DomainError, T>): ValidationResult<T> {
        return either.fold(
            { error -> ValidationResult.Failure(nonEmptyListOf(error)) },
            { value -> ValidationResult.Success(value) }
        )
    }

    /**
     * Async-aware flatMap operation for ValidationResult.
     *
     * @param validation Initial ValidationResult
     * @param transform Suspend function to transform successful values
     * @return ValidationResult after applying async transformation
     */
    suspend fun <T, U> suspendFlatMap(
        validation: ValidationResult<T>,
        transform: suspend (T) -> ValidationResult<U>
    ): ValidationResult<U> {
        return when (validation) {
            is ValidationResult.Success -> transform(validation.value)
            is ValidationResult.Failure -> ValidationResult.Failure(validation.errors)
        }
    }

    /**
     * Batch processes a list of items using ValidationResult operations.
     * Accumulates all errors or returns all successful results.
     *
     * @param items Items to process
     * @param processor Function to process each item
     * @return ValidationResult containing all processed values or accumulated errors
     */
    fun <T, U> batchProcess(
        items: List<T>,
        processor: (T) -> ValidationResult<U>
    ): ValidationResult<List<U>> {
        val results = items.map(processor)
        return sequenceToEither(results).fold(
            { errors -> ValidationResult.Failure(errors) },
            { values -> ValidationResult.Success(values) }
        )
    }
}
