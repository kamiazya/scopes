package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Base interface for command handlers in the application layer.
 *
 * Following CQRS principles, command handlers:
 * - Handle write operations that modify state
 * - Execute within transaction boundaries
 * - Validate business rules and invariants
 * - Publish domain events (future enhancement)
 * - Return minimal success indicators or detailed error information
 *
 * @param C Command type (input)
 * @param R Result type (output, usually minimal for commands)
 */
fun interface CommandHandler<C, R> {
    /**
     * Execute the command with the given input.
     *
     * @param command The command to process
     * @return Either an error or the successful result
     */
    suspend operator fun invoke(command: C): Either<ScopesError, R>
}
