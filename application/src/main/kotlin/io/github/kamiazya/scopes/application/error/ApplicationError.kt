package io.github.kamiazya.scopes.application.error

import arrow.core.Either
import arrow.core.NonEmptyList
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.fold

/**
 * Application-level errors that can occur during use case execution.
 * These wrap domain and infrastructure errors with additional context.
 */
sealed class ApplicationError {

    /**
     * Domain-related errors wrapped with application context.
     */
    data class Domain(val cause: DomainError) : ApplicationError()

    /**
     * Multiple domain validation errors accumulated together.
     * Used for error accumulation in validation scenarios.
     */
    data class ValidationFailure(val errors: NonEmptyList<DomainError>) : ApplicationError()

    /**
     * Repository/Infrastructure-related errors.
     */
    data class Repository(
        val cause: RepositoryError
    ) : ApplicationError()

    /**
     * Use case specific errors.
     */
    sealed class UseCaseError : ApplicationError() {
        data class InvalidRequest(val message: String) : UseCaseError()
        data class AuthorizationFailed(val operation: String, val reason: String) : UseCaseError()
        data class ConcurrencyConflict(val entityId: String, val message: String) : UseCaseError()
    }

    /**
     * External service integration errors.
     */
    sealed class IntegrationError : ApplicationError() {
        data class ServiceUnavailable(val serviceName: String, val cause: Throwable) : IntegrationError()
        data class ServiceTimeout(val serviceName: String, val timeoutMs: Long) : IntegrationError()
        data class InvalidResponse(val serviceName: String, val message: String) : IntegrationError()
    }

    companion object {
        /**
         * Convert domain error to application error.
         */
        fun fromDomainError(domain: DomainError): ApplicationError =
            Domain(domain)

        /**
         * Convert repository error to application error.
         */
        fun fromRepositoryError(repository: RepositoryError): ApplicationError =
            Repository(repository)

        /**
         * Convert ValidationResult to ApplicationError Either.
         * A key utility for error handling.
         */
        fun <T> fromValidationResult(result: ValidationResult<T>): Either<ApplicationError, T> =
            result.fold(
                ifFailure = { errors: NonEmptyList<DomainError> ->
                    if (errors.size == 1) {
                        Either.Left(Domain(errors.head))
                    } else {
                        Either.Left(ValidationFailure(errors))
                    }
                },
                ifSuccess = { value: T -> Either.Right(value) }
            )

    }
}
