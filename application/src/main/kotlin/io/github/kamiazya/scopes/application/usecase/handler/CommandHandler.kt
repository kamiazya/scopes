package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import io.github.kamiazya.scopes.application.dto.DTO
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.usecase.command.Command

/**
 * Base interface for command handlers in the application layer.
 * Command handlers execute write operations that modify state.
 * 
 * @param C The command type (input)
 * @param R The result type (output)
 */
interface CommandHandler<in C : Command, out R : DTO> {
    /**
     * Execute the command and return the result.
     * 
     * @param command The command to execute
     * @return Either an error or the result
     */
    suspend operator fun invoke(command: C): Either<ApplicationError, R>
}