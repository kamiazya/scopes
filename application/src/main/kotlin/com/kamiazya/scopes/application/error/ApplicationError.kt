package com.kamiazya.scopes.application.error

import com.kamiazya.scopes.domain.error.DomainError
import com.kamiazya.scopes.domain.error.RepositoryError

/**
 * Application-level errors that can occur during use case execution.
 * These wrap domain and infrastructure errors with additional context.
 */
sealed class ApplicationError {

    /**
     * Domain-related errors wrapped with application context.
     */
    data class DomainError(val domainError: com.kamiazya.scopes.domain.error.DomainError) : ApplicationError()

    /**
     * Repository/Infrastructure-related errors.
     */
    data class RepositoryError(
        val repositoryError: com.kamiazya.scopes.domain.error.RepositoryError
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
        fun fromDomainError(domainError: com.kamiazya.scopes.domain.error.DomainError): ApplicationError =
            DomainError(domainError)

        /**
         * Convert repository error to application error.
         */
        fun fromRepositoryError(repositoryError: com.kamiazya.scopes.domain.error.RepositoryError): ApplicationError =
            RepositoryError(repositoryError)
    }
}
