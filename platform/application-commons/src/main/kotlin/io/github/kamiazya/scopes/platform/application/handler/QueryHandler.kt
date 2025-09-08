package io.github.kamiazya.scopes.platform.application.handler

import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.usecase.UseCase

/**
 * Base interface for query handlers across all bounded contexts.
 *
 * Queries represent read-only operations that should:
 * - Never modify state
 * - Be optimized for read performance
 * - Support eventual consistency when reading from projections
 * - Return rich data structures for presentation
 *
 * This extends UseCase to maintain compatibility while adding semantic meaning
 * for CQRS query operations.
 *
 * @param Q Query type (input)
 * @param E Error type (specific to the bounded context)
 * @param R Result type (output, usually rich data for queries)
 */
interface QueryHandler<Q, E, R> : UseCase<Q, E, R> {
    /**
     * Execute the query with the given input.
     *
     * @param query The query to process
     * @return Either an error or the query result
     */
    override suspend operator fun invoke(input: Q): Either<E, R>
}
