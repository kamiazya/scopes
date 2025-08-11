package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlin.reflect.KClass

/**
 * General domain service error hierarchy for operational and infrastructure concerns.
 * 
 * This sealed class hierarchy provides detailed context for domain service operations
 * that go beyond validation and business rules, focusing on operational reliability
 * and external integrations.
 * 
 * Based on Serena MCP research insights for functional domain modeling where
 * operational errors require different handling strategies than business rule violations.
 */
sealed class DomainServiceError

/**
 * Service operation specific errors with detailed context.
 */
sealed class ServiceOperationError : DomainServiceError() {
    
    /**
     * Represents a service unavailable error.
     * 
     * @param serviceName The name of the service that is unavailable
     * @param reason The reason for unavailability
     * @param estimatedRecoveryTime Optional estimated recovery time in milliseconds
     * @param alternativeService Optional alternative service that can be used
     */
    data class ServiceUnavailable(
        val serviceName: String,
        val reason: String,
        val estimatedRecoveryTime: Long? = null,
        val alternativeService: String? = null
    ) : ServiceOperationError()
    
    /**
     * Represents a service timeout error.
     * 
     * @param serviceName The name of the service that timed out
     * @param operation The operation that timed out
     * @param timeoutMillis The timeout duration in milliseconds
     * @param elapsedMillis The actual elapsed time before timeout
     */
    data class ServiceTimeout(
        val serviceName: String,
        val operation: String,
        val timeoutMillis: Long,
        val elapsedMillis: Long
    ) : ServiceOperationError()
}

/**
 * Repository integration specific errors with detailed context.
 */
sealed class RepositoryIntegrationError : DomainServiceError() {
    
    /**
     * Represents a repository operation failure.
     * 
     * @param operation The repository operation that failed
     * @param repositoryName The name of the repository
     * @param cause The underlying cause of the failure
     * @param retryable Whether the operation can be retried
     */
    data class RepositoryOperationFailure(
        val operation: String,
        val repositoryName: String,
        val cause: Throwable,
        val retryable: Boolean = false
    ) : RepositoryIntegrationError()
}

/**
 * External service integration specific errors with detailed context.
 */
sealed class ExternalServiceError : DomainServiceError() {
    
    /**
     * Represents an external service integration failure.
     * 
     * @param serviceName The name of the external service
     * @param endpoint The service endpoint that failed
     * @param statusCode Optional HTTP status code if applicable
     * @param errorMessage The error message from the external service
     * @param retryAfter Optional retry-after duration in seconds
     */
    data class IntegrationFailure(
        val serviceName: String,
        val endpoint: String,
        val statusCode: Int? = null,
        val errorMessage: String,
        val retryAfter: Int? = null
    ) : ExternalServiceError()
}