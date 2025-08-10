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
     * Translates database adapter errors to save operation errors.
     */
    fun translateToSaveError(
        error: InfrastructureAdapterError.DatabaseAdapterError,
        scopeId: ScopeId
    ): SaveScopeError = when (error) {
        is InfrastructureAdapterError.DatabaseAdapterError.ConnectionError -> 
            SaveScopeError.DatabaseError(
                scopeId = scopeId,
                message = "Database connection failed: ${error.cause.message}",
                cause = error.cause,
                retryable = error.retryable
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.QueryError -> 
            SaveScopeError.DatabaseError(
                scopeId = scopeId,
                message = "Query execution failed: ${error.cause.message}",
                cause = error.cause,
                retryable = error.retryable
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
            SaveScopeError.DatabaseError(
                scopeId = scopeId,
                message = "Database resource exhausted: ${error.resource}",
                cause = error.cause,
                retryable = error.retryable
            )
    }

    /**
     * Translates database adapter errors to existence check errors.
     */
    fun translateToExistsError(
        error: InfrastructureAdapterError.DatabaseAdapterError,
        scopeId: ScopeId? = null
    ): ExistsScopeError = when (error) {
        is InfrastructureAdapterError.DatabaseAdapterError.ConnectionError -> 
            ExistsScopeError.ConnectionFailure(
                message = "Database connection failed: ${error.cause.message}",
                cause = error.cause
            )
        
        is InfrastructureAdapterError.DatabaseAdapterError.QueryError -> 
            ExistsScopeError.QueryTimeout(
                scopeId = scopeId ?: ScopeId.generate(),
                timeoutMs = error.executionTimeMs ?: 0,
                operation = "existence check"
            )
        
        else -> ExistsScopeError.UnknownError(
            message = "Infrastructure error: Unknown database error",
            cause = RuntimeException("Unknown database adapter error")
        )
    }

    /**
     * Translates database adapter errors to count operation errors.
     */
    fun translateToCountError(
        error: InfrastructureAdapterError.DatabaseAdapterError,
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
                timeoutMillis = error.executionTimeMs ?: 0
            )
        
        else -> CountScopeError.UnknownError(
            parentId = parentId ?: ScopeId.generate(),
            message = "Infrastructure error: Unknown database error",
            cause = RuntimeException("Unknown database adapter error")
        )
    }

    /**
     * Translates database adapter errors to find operation errors.
     */
    fun translateToFindError(
        error: InfrastructureAdapterError.DatabaseAdapterError,
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
                timeoutMillis = error.executionTimeMs ?: 0
            )
        
        else -> FindScopeError.UnknownError(
            scopeId = scopeId ?: ScopeId.generate(),
            message = "Infrastructure error: Unknown database error",
            cause = RuntimeException("Unknown database adapter error")
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