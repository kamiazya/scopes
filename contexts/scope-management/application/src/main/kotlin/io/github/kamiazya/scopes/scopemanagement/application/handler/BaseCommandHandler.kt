package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError

/**
 * Abstract base class for scope management command handlers.
 * Provides common functionality including:
 * - Standardized logging
 * - Transaction management
 * - Error handling patterns
 * - Template method pattern for consistent structure
 */
abstract class BaseCommandHandler<C, R>(protected val transactionManager: TransactionManager, protected val logger: Logger) :
    CommandHandler<C, ScopeManagementApplicationError, R> {

    /**
     * Template method that provides common structure for all commands.
     * Subclasses implement executeCommand for specific business logic.
     */
    override suspend operator fun invoke(command: C): Either<ScopeManagementApplicationError, R> = either {
        logCommandStart(command)

        val result = transactionManager.inTransaction {
            executeCommand(command)
        }.bind()

        logCommandSuccess(command, result)
        result
    }.onLeft { error ->
        logCommandError(command, error)
    }

    /**
     * Abstract method for subclasses to implement specific command logic.
     * This runs within a transaction boundary.
     */
    protected abstract suspend fun executeCommand(command: C): Either<ScopeManagementApplicationError, R>

    /**
     * Template method for command start logging.
     * Subclasses can override for specific logging needs.
     */
    protected open fun logCommandStart(command: C) {
        logger.info(
            "Executing command",
            mapOf(
                "command" to getCommandName(command),
                "commandType" to (command!!::class.simpleName ?: "Unknown"),
            ),
        )
    }

    /**
     * Template method for success logging.
     * Subclasses can override for specific logging needs.
     */
    protected open fun logCommandSuccess(command: C, result: R) {
        logger.info(
            "Command executed successfully",
            mapOf(
                "command" to getCommandName(command),
                "commandType" to (command!!::class.simpleName ?: "Unknown"),
            ),
        )
    }

    /**
     * Template method for error logging.
     * Subclasses can override for specific logging needs.
     */
    protected open fun logCommandError(command: C, error: ScopeManagementApplicationError) {
        logger.error(
            "Command execution failed",
            mapOf(
                "command" to getCommandName(command),
                "commandType" to (command!!::class.simpleName ?: "Unknown"),
                "errorCode" to getErrorClassName(error),
                "errorMessage" to error.toString().take(500),
            ),
        )
    }

    /**
     * Get a meaningful command name for logging.
     * Subclasses can override for better naming.
     */
    protected open fun getCommandName(command: C): String = command!!::class.simpleName ?: "UnknownCommand"

    /**
     * Get error class name for consistent error logging.
     */
    protected fun getErrorClassName(error: ScopeManagementApplicationError): String = error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"
}
