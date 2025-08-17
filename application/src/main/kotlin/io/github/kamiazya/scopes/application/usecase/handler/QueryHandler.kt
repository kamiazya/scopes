package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import io.github.kamiazya.scopes.application.dto.DTO
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.usecase.query.Query

/**
 * Base interface for query handlers in the application layer.
 * Query handlers execute read operations that retrieve state.
 * 
 * @param Q The query type (input)
 * @param R The result type (output)
 */
interface QueryHandler<in Q : Query, out R : DTO> {
    /**
     * Execute the query and return the result.
     * 
     * @param query The query to execute
     * @return Either an error or the result
     */
    suspend operator fun invoke(query: Q): Either<ApplicationError, R>
}