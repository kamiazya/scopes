package io.github.kamiazya.scopes.infrastructure.error

import io.github.kamiazya.scopes.domain.error.*
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Translates infrastructure-specific adapter errors to domain repository errors.
 * Maintains Clean Architecture boundaries by abstracting infrastructure failures
 * into domain-meaningful error types.
 */
class InfrastructureErrorTranslator {

    /**
     * Translates infrastructure adapter errors to save operation errors.
     */
    fun translateToSaveError(
        error: InfrastructureAdapterError,
        scopeId: ScopeId
    ): SaveScopeError = when (error) {
        is InfrastructureAdapterError.DatabaseAdapterError.ConnectionError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "Database connection error",
                cause = error.cause,
                retryable = error.retryable,
                correlationId = error.correlationId
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.QueryError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "Database query error",
                cause = error.cause,
                retryable = error.retryable,
                correlationId = error.correlationId
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.TransactionError -> 
            SaveScopeError.TransactionError(
                scopeId = scopeId,
                transactionId = error.transactionId,
                operation = error.operation.name,
                retryable = error.retryable,
                cause = error.cause
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.DataIntegrityError -> 
            SaveScopeError.DuplicateId(scopeId)
        
        is InfrastructureAdapterError.DatabaseAdapterError.ResourceExhaustionError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "Database resource exhausted",
                cause = error.cause,
                retryable = error.retryable,
                correlationId = error.correlationId
            )
            
        is InfrastructureAdapterError.ExternalApiAdapterError.NetworkError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "External API network error",
                cause = error.cause,
                retryable = true,
                correlationId = error.correlationId
            )
            
        is InfrastructureAdapterError.ExternalApiAdapterError.HttpError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "External API HTTP error (${error.statusCode})",
                cause = RuntimeException("HTTP ${error.statusCode}: ${error.responseBody}"),
                retryable = error.statusCode >= 500,
                correlationId = error.correlationId
            )
            
        is InfrastructureAdapterError.FileSystemAdapterError.PermissionError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "Filesystem permission error",
                cause = RuntimeException("Permission denied: ${error.path}"),
                retryable = false,
                correlationId = error.correlationId
            )
            
        is InfrastructureAdapterError.FileSystemAdapterError.StorageError -> 
            SaveScopeError.StorageError(
                scopeId = scopeId,
                availableSpace = error.availableSpace ?: 0L,
                requiredSpace = error.requiredSpace ?: 0L,
                retryable = false
            )
            
        is InfrastructureAdapterError.MessagingAdapterError.DeliveryError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "Messaging delivery error",
                cause = error.cause,
                retryable = true,
                correlationId = error.correlationId
            )
            
        is InfrastructureAdapterError.ConfigurationAdapterError.ValidationError -> 
            SaveScopeError.InfrastructureError(
                scopeId = scopeId,
                failureType = "Configuration validation error",
                cause = RuntimeException("Config validation failed: ${error.configKey}"),
                retryable = false,
                correlationId = error.correlationId
            )
            
        is InfrastructureAdapterError.TransactionAdapterError.DeadlockError -> 
            SaveScopeError.TransactionError(
                scopeId = scopeId,
                transactionId = error.transactionId,
                operation = "DEADLOCK",
                retryable = true,
                cause = RuntimeException("Deadlock detected")
            )
            
        else -> SaveScopeError.InfrastructureError(
            scopeId = scopeId,
            failureType = "Unknown infrastructure error",
            cause = RuntimeException("Unknown infrastructure adapter error"),
            retryable = false,
            correlationId = null
        )
    }

    /**
     * Translates infrastructure adapter errors to existence check errors.
     */
    fun translateToExistsError(
        error: InfrastructureAdapterError,
        scopeId: ScopeId? = null
    ): ExistsScopeError = when (error) {
        is InfrastructureAdapterError.DatabaseAdapterError.ConnectionError -> 
            ExistsScopeError.ConnectionFailure(
                message = "Database connection failed: ${error.cause.message}",
                cause = error.cause
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.QueryError -> 
            ExistsScopeError.PersistenceFailure(
                message = "Query execution failed",
                cause = error.cause
            )
        
        else -> ExistsScopeError.PersistenceFailure(
            message = "Infrastructure error: Unknown adapter error",
            cause = RuntimeException("Unknown infrastructure adapter error")
        )
    }

    /**
     * Translates infrastructure adapter errors to count operation errors.
     */
    fun translateToCountError(
        error: InfrastructureAdapterError,
        parentId: ScopeId?
    ): CountScopeError = when (error) {
        is InfrastructureAdapterError.DatabaseAdapterError.ConnectionError -> 
            CountScopeError.ConnectionError(
                parentId = parentId ?: ScopeId.generate(),
                retryable = error.retryable,
                cause = error.cause
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.QueryError -> 
            CountScopeError.AggregationTimeout(
                timeoutMillis = error.executionTimeMs ?: 0L
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.ResourceExhaustionError -> 
            CountScopeError.ConnectionError(
                parentId = parentId ?: ScopeId.generate(),
                retryable = error.retryable,
                cause = error.cause
            )
        
        else -> CountScopeError.UnknownError(
            parentId = parentId ?: ScopeId.generate(),
            message = "Infrastructure error: Unknown adapter error",
            cause = RuntimeException("Unknown infrastructure adapter error")
        )
    }

    /**
     * Translates infrastructure adapter errors to find operation errors.
     */
    fun translateToFindError(
        error: InfrastructureAdapterError,
        scopeId: ScopeId?
    ): FindScopeError = when (error) {
        is InfrastructureAdapterError.DatabaseAdapterError.ConnectionError -> 
            FindScopeError.ConnectionFailure(
                scopeId = scopeId ?: ScopeId.generate(),
                message = "Database connection failed: ${error.cause.message}",
                cause = error.cause
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.QueryError -> 
            FindScopeError.TraversalTimeout(
                scopeId = scopeId ?: ScopeId.generate(),
                timeoutMillis = error.executionTimeMs ?: 0L
            )
        
        else -> FindScopeError.UnknownError(
            scopeId = scopeId ?: ScopeId.generate(),
            message = "Infrastructure error: Unknown adapter error",
            cause = RuntimeException("Unknown infrastructure adapter error")
        )
    }

    /**
     * Translates external API adapter errors to general infrastructure errors.
     */
    fun translateExternalApiError(
        error: InfrastructureAdapterError.ExternalApiAdapterError
    ): String = when (error) {
        is InfrastructureAdapterError.ExternalApiAdapterError.NetworkError -> 
            "Network error: ${error.networkType} for ${error.endpoint}"
        
        is InfrastructureAdapterError.ExternalApiAdapterError.HttpError -> 
            "HTTP ${error.statusCode} error for ${error.endpoint}"
        
        is InfrastructureAdapterError.ExternalApiAdapterError.TimeoutError -> 
            "Request timeout (${error.timeoutMs}ms) for ${error.endpoint}"
        
        is InfrastructureAdapterError.ExternalApiAdapterError.CircuitBreakerError -> 
            "Circuit breaker ${error.state} for ${error.endpoint}"
        
        is InfrastructureAdapterError.ExternalApiAdapterError.RateLimitError -> 
            "Rate limit exceeded for ${error.endpoint}"
        
        is InfrastructureAdapterError.ExternalApiAdapterError.ServiceUnavailableError -> 
            "Service ${error.serviceName} unavailable"
    }
}