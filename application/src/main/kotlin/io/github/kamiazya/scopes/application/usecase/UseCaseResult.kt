package io.github.kamiazya.scopes.application.usecase

import io.github.kamiazya.scopes.application.error.ApplicationError

/**
 * Unified result type for all UseCase implementations.
 * Provides consistent error handling and success representation across the application.
 * 
 * This sealed interface replaces the direct use of Either<ApplicationError, T> to:
 * - Strengthen type safety through domain-specific result types
 * - Provide consistent naming conventions (Ok/Err vs Left/Right)
 * - Simplify result handling in presentation layers
 * - Enable future extensions (metrics, tracing, etc.)
 * 
 * @param T The success value type (typically a DTO)
 */
sealed interface UseCaseResult<out T> {
    
    /**
     * Represents a successful use case execution.
     * 
     * @param value The success result value
     */
    data class Ok<T>(val value: T) : UseCaseResult<T>
    
    /**
     * Represents a failed use case execution.
     * 
     * @param error The application error that occurred
     */
    data class Err(val error: ApplicationError) : UseCaseResult<Nothing>
    
    companion object {
        /**
         * Create a successful result.
         */
        fun <T> ok(value: T): UseCaseResult<T> = Ok(value)
        
        /**
         * Create an error result.
         */
        fun err(error: ApplicationError): UseCaseResult<Nothing> = Err(error)
    }
}

/**
 * Transform the result value if successful, otherwise return the error unchanged.
 * 
 * @param transform Function to apply to the success value
 * @return Transformed result or original error
 */
inline fun <T, R> UseCaseResult<T>.map(transform: (T) -> R): UseCaseResult<R> = when (this) {
    is UseCaseResult.Ok -> UseCaseResult.Ok(transform(value))
    is UseCaseResult.Err -> this
}

/**
 * Apply a transformation that returns a UseCaseResult, flattening nested results.
 * 
 * @param transform Function that returns a UseCaseResult
 * @return Flattened result
 */
inline fun <T, R> UseCaseResult<T>.flatMap(transform: (T) -> UseCaseResult<R>): UseCaseResult<R> = when (this) {
    is UseCaseResult.Ok -> transform(value)
    is UseCaseResult.Err -> this
}

/**
 * Apply different functions based on whether the result is Ok or Err.
 * 
 * @param ifOk Function to apply to success value
 * @param ifErr Function to apply to error
 * @return The result of applying the appropriate function
 */
inline fun <T, R> UseCaseResult<T>.fold(ifOk: (T) -> R, ifErr: (ApplicationError) -> R): R = when (this) {
    is UseCaseResult.Ok -> ifOk(value)
    is UseCaseResult.Err -> ifErr(error)
}

/**
 * Get the success value if present, otherwise return null.
 */
fun <T> UseCaseResult<T>.getOrNull(): T? = when (this) {
    is UseCaseResult.Ok -> value
    is UseCaseResult.Err -> null
}

/**
 * Get the success value if present, otherwise return the provided default.
 */
inline fun <T> UseCaseResult<T>.getOrElse(default: () -> T): T = when (this) {
    is UseCaseResult.Ok -> value
    is UseCaseResult.Err -> default()
}

/**
 * Check if the result represents success.
 */
fun <T> UseCaseResult<T>.isOk(): Boolean = this is UseCaseResult.Ok

/**
 * Check if the result represents an error.
 */
fun <T> UseCaseResult<T>.isErr(): Boolean = this is UseCaseResult.Err
