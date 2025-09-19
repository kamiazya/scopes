package io.github.kamiazya.scopes.platform.application.error

import arrow.core.Either
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Generic error mapper interface for mapping between domain errors and contract errors.
 *
 * This interface provides a consistent pattern for error mapping across all bounded contexts.
 * Each bounded context should implement this interface to provide specific mappings
 * from their domain errors to their contract errors.
 *
 * @param TDomainError The domain error type (e.g., ScopesError, EventStoreError)
 * @param TContractError The contract error type (e.g., ScopeContractError, EventStoreContractError)
 */
interface ErrorMapper<TDomainError : Any, TContractError : Any> {
    /**
     * Maps a domain error to a contract error.
     *
     * @param domainError The domain error to map
     * @return The corresponding contract error
     */
    fun mapToContractError(domainError: TDomainError): TContractError

    /**
     * Maps a domain result to a contract result.
     * This is a convenience method that handles Either types automatically.
     *
     * @param domainResult The domain result to map
     * @return The corresponding contract result
     */
    fun <T> mapResult(domainResult: Either<TDomainError, T>): Either<TContractError, T> = domainResult.mapLeft { mapToContractError(it) }
}

/**
 * Base implementation of ErrorMapper that provides common logging for unmapped errors.
 *
 * Bounded contexts should extend this class instead of implementing ErrorMapper directly
 * to get consistent error logging behavior.
 *
 * @param TDomainError The domain error type
 * @param TContractError The contract error type (must have a SystemError.ServiceUnavailable type)
 */
abstract class BaseErrorMapper<TDomainError : Any, TContractError : Any>(protected val logger: Logger) : ErrorMapper<TDomainError, TContractError> {

    /**
     * Returns the service name for this error mapper.
     * Override this in concrete implementations to provide the correct service name.
     */
    protected abstract fun getServiceName(): String

    /**
     * Maps to a system error indicating service unavailability.
     * Concrete implementations should ensure TContractError has this capability.
     */
    protected abstract fun createServiceUnavailableError(serviceName: String): TContractError

    /**
     * Default implementation for mapping to system error.
     * Can be overridden if different behavior is needed.
     */
    protected open fun mapSystemError(): TContractError = createServiceUnavailableError(getServiceName())

    /**
     * Logs an unmapped error and returns a fallback error.
     * This should be called in the else clause of when expressions in mapToContractError.
     *
     * @param unmappedError The error that couldn't be mapped
     * @param fallbackError The fallback error to return
     * @return The fallback error
     */
    protected fun handleUnmappedError(unmappedError: TDomainError, fallbackError: TContractError): TContractError {
        logger.error(
            "Unmapped domain error encountered, using fallback mapping",
            mapOf(
                "errorClass" to unmappedError::class.simpleName.orEmpty(),
                "errorMessage" to unmappedError.toString(),
                "errorType" to unmappedError::class.qualifiedName.orEmpty(),
                "fallbackError" to fallbackError::class.simpleName.orEmpty(),
            ),
        )
        return fallbackError
    }
}

/**
 * Error mapper for mapping between different domain error types.
 *
 * This is useful when one bounded context needs to handle errors from another
 * bounded context (e.g., scope-management handling event-store errors).
 */
interface CrossContextErrorMapper<TSourceError : Any, TTargetError : Any> {
    /**
     * Maps an error from the source context to the target context.
     *
     * @param sourceError The source error to map
     * @return The corresponding target error
     */
    fun mapCrossContext(sourceError: TSourceError): TTargetError

    /**
     * Maps a source result to a target result.
     *
     * @param sourceResult The source result to map
     * @return The corresponding target result
     */
    fun <T> mapCrossContextResult(sourceResult: Either<TSourceError, T>): Either<TTargetError, T> = sourceResult.mapLeft { mapCrossContext(it) }
}

/**
 * Base implementation for cross-context error mapping with logging support.
 */
abstract class BaseCrossContextErrorMapper<TSourceError : Any, TTargetError : Any>(protected val logger: Logger) :
    CrossContextErrorMapper<TSourceError, TTargetError> {

    /**
     * Logs an unmapped cross-context error and returns a fallback error.
     *
     * @param unmappedError The error that couldn't be mapped
     * @param fallbackError The fallback error to return
     * @return The fallback error
     */
    protected fun handleUnmappedCrossContextError(unmappedError: TSourceError, fallbackError: TTargetError): TTargetError {
        logger.error(
            "Unmapped cross-context error encountered, using fallback mapping",
            mapOf(
                "sourceErrorClass" to unmappedError::class.simpleName.orEmpty(),
                "sourceErrorMessage" to unmappedError.toString(),
                "sourceErrorType" to unmappedError::class.qualifiedName.orEmpty(),
                "targetErrorClass" to fallbackError::class.simpleName.orEmpty(),
            ),
        )
        return fallbackError
    }
}
