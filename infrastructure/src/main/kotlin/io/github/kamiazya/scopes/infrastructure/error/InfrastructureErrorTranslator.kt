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
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.DatabaseConnectionError(
                    details = error.cause.message ?: "Connection failed"
                ),
                retryable = error.retryable,
                correlationId = error.correlationId,
                cause = error.cause
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.QueryError -> 
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.DatabaseQueryError(
                    query = error.query
                ),
                retryable = error.retryable,
                correlationId = error.correlationId,
                cause = error.cause
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
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.ResourceExhaustedError(
                    resource = "database"
                ),
                retryable = error.retryable,
                correlationId = error.correlationId,
                cause = error.cause
            )
            
        is InfrastructureAdapterError.ExternalApiAdapterError.NetworkError -> 
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.NetworkError(
                    endpoint = error.endpoint
                ),
                retryable = true,
                correlationId = error.correlationId,
                cause = error.cause
            )
            
        is InfrastructureAdapterError.ExternalApiAdapterError.HttpError -> 
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.ExternalServiceError(
                    serviceName = error.endpoint,
                    statusCode = error.statusCode
                ),
                retryable = error.statusCode >= 500,
                correlationId = error.correlationId,
                cause = RuntimeException("HTTP ${error.statusCode}: ${error.responseBody}")
            )
            
        is InfrastructureAdapterError.FileSystemAdapterError.PermissionError -> 
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.FileSystemError(
                    path = error.path,
                    operation = "permission_check"
                ),
                retryable = false,
                correlationId = error.correlationId,
                cause = RuntimeException("Permission denied: ${error.path}")
            )
            
        is InfrastructureAdapterError.FileSystemAdapterError.StorageError -> 
            SaveScopeError.StorageError(
                scopeId = scopeId,
                availableSpace = error.availableSpace ?: 0L,
                requiredSpace = error.requiredSpace ?: 0L,
                retryable = false
            )
            
        is InfrastructureAdapterError.MessagingAdapterError.DeliveryError -> 
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.MessagingError(
                    messageType = "delivery"
                ),
                retryable = true,
                correlationId = error.correlationId,
                cause = error.cause
            )
            
        is InfrastructureAdapterError.ConfigurationAdapterError.ValidationError -> 
            SaveScopeError.SystemFailure(
                scopeId = scopeId,
                failure = SaveScopeError.SystemFailure.SystemFailureType.ConfigurationError(
                    configKey = error.configKey
                ),
                retryable = false,
                correlationId = error.correlationId,
                cause = RuntimeException("Config validation failed: ${error.configKey}")
            )
            
        is InfrastructureAdapterError.TransactionAdapterError.DeadlockError -> 
            SaveScopeError.TransactionError(
                scopeId = scopeId,
                transactionId = error.transactionId,
                operation = "DEADLOCK",
                retryable = true,
                cause = RuntimeException("Deadlock detected")
            )
            
        else -> SaveScopeError.SystemFailure(
            scopeId = scopeId,
            failure = SaveScopeError.SystemFailure.SystemFailureType.UnknownError(
                description = "Unknown infrastructure adapter error"
            ),
            retryable = false,
            correlationId = null,
            cause = RuntimeException("Unknown infrastructure adapter error")
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
            ExistsScopeError.PersistenceError(
                context = ExistsScopeError.ExistenceContext.ByCustomCriteria(mapOf("operation" to "query")),
                message = "Query execution failed",
                cause = error.cause
            )
        
        else -> ExistsScopeError.PersistenceError(
            context = ExistsScopeError.ExistenceContext.ByCustomCriteria(mapOf("operation" to "unknown")),
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