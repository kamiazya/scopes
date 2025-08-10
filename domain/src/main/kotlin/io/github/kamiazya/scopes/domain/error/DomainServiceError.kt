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
sealed class DomainServiceError {

    /**
     * Service operation specific errors with detailed context.
     */
    sealed class ServiceOperationError : DomainServiceError() {
        
        /**
         * Represents a service unavailable error.
         * 
         * @param serviceName The name of the service that is unavailable
         * @param reason The reason why the service is unavailable
         */
        data class ServiceUnavailable(
            val serviceName: String,
            val reason: String
        ) : ServiceOperationError()
        
        /**
         * Represents an operation timeout error.
         * 
         * @param operation The operation that timed out
         * @param timeoutMs The timeout duration in milliseconds
         */
        data class OperationTimeout(
            val operation: String,
            val timeoutMs: Long
        ) : ServiceOperationError()
        
        /**
         * Represents a configuration error.
         * 
         * @param configKey The configuration key that has an invalid value
         * @param expectedType The expected type of the configuration value
         * @param actualType The actual type of the provided value
         * @param redactedPreview Optional redacted preview of the actual value (first 10 chars + "...")
         */
        data class ConfigurationError(
            val configKey: String,
            val expectedType: KClass<*>,
            val actualType: KClass<*>,
            val redactedPreview: String? = null
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
         * @param repositoryError The underlying repository error
         */
        data class OperationFailed(
            val operation: String,
            val repositoryError: RepositoryError
        ) : RepositoryIntegrationError()
        
        /**
         * Represents a data consistency error from repository operations.
         * 
         * @param scopeId The scope ID where data inconsistency was detected
         * @param inconsistencyType The type of data inconsistency
         * @param details Additional details about the inconsistency
         */
        data class DataConsistencyError(
            val scopeId: ScopeId,
            val inconsistencyType: String,
            val details: String
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
         * @param operation The operation that failed on the external service
         * @param errorCode The error code from the external service
         */
        data class IntegrationFailure(
            val serviceName: String,
            val operation: String,
            val errorCode: String
        ) : ExternalServiceError()
        
        /**
         * Represents an external service authentication failure.
         * 
         * @param serviceName The name of the external service
         * @param reason The reason for authentication failure
         */
        data class AuthenticationFailure(
            val serviceName: String,
            val reason: String
        ) : ExternalServiceError()
    }
}