package io.github.kamiazya.scopes.platform.application.handler

import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.usecase.UseCase

/**
 * Base interface for command handlers across all bounded contexts.
 *
 * Commands represent operations that modify state and should:
 * - Execute within transaction boundaries
 * - Validate business rules and invariants
 * - Publish domain events (when event sourcing is implemented)
 * - Return minimal success indicators or detailed error information
 *
 * This extends UseCase to maintain compatibility while adding semantic meaning
 * for CQRS command operations.
 *
 * @param C Command type (input)
 * @param E Error type (specific to the bounded context)
 * @param R Result type (output, usually minimal for commands)
 */
interface CommandHandler<C, E, R> : UseCase<C, E, R> {
    /**
     * Execute the command with the given input.
     *
     * @param command The command to process
     * @return Either an error or the successful result
     */
    override suspend operator fun invoke(input: C): Either<E, R>
}
