package io.github.kamiazya.scopes.scopemanagement.application.usecase

import arrow.core.Either

/**
 * Base interface for use cases in the application layer.
 *
 * Following Clean Architecture and DDD principles:
 * - Use cases orchestrate domain logic
 * - Each use case represents a single business capability
 * - Use cases are transaction boundaries
 * - Use cases don't depend on external frameworks
 *
 * @param I Input type (Command or Query)
 * @param E Error type (domain or application error)
 * @param T Success result type (DTO)
 */
fun interface UseCase<I, E, T> {
    /**
     * Execute the use case with the given input.
     *
     * @param input The command or query to process
     * @return Either an error or the successful result
     */
    suspend operator fun invoke(input: I): Either<E, T>
}
