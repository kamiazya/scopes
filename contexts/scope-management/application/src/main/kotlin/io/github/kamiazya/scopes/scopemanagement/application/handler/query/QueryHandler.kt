package io.github.kamiazya.scopes.scopemanagement.application.handler.query

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Base interface for query handlers in the application layer.
 *
 * Following CQRS principles, query handlers:
 * - Handle read operations without side effects
 * - Do not require transaction boundaries (read-only)
 * - Can use optimized read models or projections
 * - Focus on data retrieval and formatting for consumers
 * - Support caching and performance optimizations
 *
 * @param Q Query type (input)
 * @param R Result type (output, rich data for queries)
 */
fun interface QueryHandler<Q, R> {
    /**
     * Execute the query with the given input.
     *
     * @param query The query to process
     * @return Either an error or the successful result
     */
    suspend operator fun invoke(query: Q): Either<ScopesError, R>
}
