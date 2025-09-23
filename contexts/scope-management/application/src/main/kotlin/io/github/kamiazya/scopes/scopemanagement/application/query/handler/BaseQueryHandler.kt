package io.github.kamiazya.scopes.scopemanagement.application.query.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.ErrorUtilityMethods

/**
 * Abstract base class for scope management query handlers.
 * Provides common functionality including:
 * - Standardized logging
 * - Read-only transaction management
 * - Error handling patterns
 * - Template method pattern for consistent structure
 */
abstract class BaseQueryHandler<Q, R>(protected val transactionManager: TransactionManager, protected val logger: Logger) :
    QueryHandler<Q, ScopeContractError, R> {

    companion object {
        private const val QUERY_NULL_ERROR = "Query must not be null"
    }

    /**
     * Template method that provides common structure for all queries.
     * Subclasses implement executeQuery for specific business logic.
     */
    override suspend operator fun invoke(query: Q): Either<ScopeContractError, R> = either {
        logQueryStart(query)

        val result = transactionManager.inReadOnlyTransaction {
            executeQuery(query)
        }.bind()

        logQuerySuccess(query, result)
        result
    }.onLeft { error ->
        logQueryError(query, error)
    }

    /**
     * Abstract method for subclasses to implement specific query logic.
     * This runs within a read-only transaction boundary.
     */
    protected abstract suspend fun executeQuery(query: Q): Either<ScopeContractError, R>

    /**
     * Template method for query start logging.
     * Subclasses can override for specific logging needs.
     */
    protected open fun logQueryStart(query: Q) {
        logger.debug(
            "Executing query",
            mapOf(
                "query" to getQueryName(query),
                "queryType" to (query?.let { it::class.simpleName } ?: error(QUERY_NULL_ERROR)),
            ),
        )
    }

    /**
     * Template method for success logging.
     * Subclasses can override for specific logging needs.
     */
    protected open fun logQuerySuccess(query: Q, result: R) {
        logger.info(
            "Query executed successfully",
            mapOf(
                "query" to getQueryName(query),
                "queryType" to (query?.let { it::class.simpleName } ?: error(QUERY_NULL_ERROR)),
            ),
        )
    }

    /**
     * Template method for error logging.
     * Subclasses can override for specific logging needs.
     */
    protected open fun logQueryError(query: Q, error: ScopeContractError) {
        logger.error(
            "Query execution failed",
            mapOf(
                "query" to getQueryName(query),
                "queryType" to (query?.let { it::class.simpleName } ?: error(QUERY_NULL_ERROR)),
                "errorCode" to getErrorClassName(error),
                "errorMessage" to error.toString().take(500),
            ),
        )
    }

    /**
     * Get a meaningful query name for logging.
     * Subclasses can override for better naming.
     */
    protected open fun getQueryName(query: Q): String = query?.let { it::class.simpleName } ?: error(QUERY_NULL_ERROR)

    /**
     * Get error class name for consistent error logging.
     * Delegates to shared utility for consistency across handlers.
     */
    protected fun getErrorClassName(error: ScopeContractError): String = ErrorUtilityMethods.getErrorClassName(error)
}
