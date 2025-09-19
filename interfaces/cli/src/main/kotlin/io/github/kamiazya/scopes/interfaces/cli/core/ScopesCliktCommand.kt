package io.github.kamiazya.scopes.interfaces.cli.core

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.interfaces.cli.exitcode.ExitCode
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper

/**
 * Base class for all Scopes CLI commands with standardized error handling and exit codes.
 *
 * This class extends CliktCommand to provide:
 * - Consistent error handling with proper exit codes
 * - Automatic mapping of contract errors to user-friendly messages
 * - Standard error formatting
 */
abstract class ScopesCliktCommand(
    help: String = "",
    epilog: String = "",
    name: String? = null,
    invokeWithoutSubcommand: Boolean = false,
    printHelpOnEmptyArgs: Boolean = false,
    helpTags: Map<String, String> = emptyMap(),
    autoCompleteEnvvar: String? = "",
    allowMultipleSubcommands: Boolean = false,
    treatUnknownOptionsAsArgs: Boolean = false,
) : CliktCommand(
    help = help,
    epilog = epilog,
    name = name,
    invokeWithoutSubcommand = invokeWithoutSubcommand,
    printHelpOnEmptyArgs = printHelpOnEmptyArgs,
    helpTags = helpTags,
    autoCompleteEnvvar = autoCompleteEnvvar,
    allowMultipleSubcommands = allowMultipleSubcommands,
    treatUnknownOptionsAsArgs = treatUnknownOptionsAsArgs,
) {
    /**
     * Throws a CliktError with the appropriate exit code and user-friendly message.
     *
     * @param message The error message to display
     * @param exitCode The exit code to use (defaults to GENERAL_ERROR)
     */
    protected fun fail(message: String, exitCode: ExitCode = ExitCode.GENERAL_ERROR): Nothing {
        // Set the exit code in the system property for Main.kt to handle
        System.setProperty("scopes.cli.exit.code", exitCode.code.toString())
        throw CliktError(message)
    }

    /**
     * Handles a contract error by converting it to a user-friendly message and throwing with appropriate exit code.
     *
     * @param error The contract error to handle
     */
    protected fun handleContractError(error: ScopeContractError): Nothing {
        val message = ContractErrorMessageMapper.getMessage(error)
        val exitCode = mapContractErrorToExitCode(error)
        fail("Error: $message", exitCode)
    }

    /**
     * Maps contract errors to appropriate exit codes.
     */
    private fun mapContractErrorToExitCode(error: ScopeContractError): ExitCode = when (error) {
        is ScopeContractError.BusinessError.NotFound -> ExitCode.SCOPE_NOT_FOUND
        is ScopeContractError.BusinessError.DuplicateAlias -> ExitCode.DUPLICATE_RESOURCE
        is ScopeContractError.BusinessError.DuplicateTitle -> ExitCode.DUPLICATE_RESOURCE
        is ScopeContractError.BusinessError.HierarchyViolation -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.BusinessError.AlreadyDeleted -> ExitCode.STATE_ERROR
        is ScopeContractError.BusinessError.ArchivedScope -> ExitCode.STATE_ERROR
        is ScopeContractError.BusinessError.NotArchived -> ExitCode.STATE_ERROR
        is ScopeContractError.BusinessError.HasChildren -> ExitCode.STATE_ERROR
        is ScopeContractError.BusinessError.AliasNotFound -> ExitCode.ALIAS_NOT_FOUND
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.BusinessError.AliasOfDifferentScope -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.BusinessError.AliasGenerationFailed -> ExitCode.UNAVAILABLE
        is ScopeContractError.BusinessError.AliasGenerationValidationFailed -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.BusinessError.ContextNotFound -> ExitCode.SCOPE_NOT_FOUND
        is ScopeContractError.BusinessError.DuplicateContextKey -> ExitCode.DUPLICATE_RESOURCE
        is ScopeContractError.InputError.InvalidTitle -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.InputError.InvalidDescription -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.InputError.InvalidId -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.InputError.InvalidParentId -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.InputError.InvalidAlias -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.InputError.InvalidContextKey -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.InputError.InvalidContextName -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.InputError.InvalidContextFilter -> ExitCode.VALIDATION_ERROR
        is ScopeContractError.DataInconsistency.MissingCanonicalAlias -> ExitCode.UNAVAILABLE
        is ScopeContractError.SystemError.ServiceUnavailable -> ExitCode.UNAVAILABLE
        is ScopeContractError.SystemError.Timeout -> ExitCode.TEMP_FAIL
        is ScopeContractError.SystemError.ConcurrentModification -> ExitCode.STATE_ERROR
    }

    /**
     * Validates a required parameter and fails with appropriate error if invalid.
     */
    protected fun requireNotBlank(value: String?, parameterName: String): String {
        if (value.isNullOrBlank()) {
            fail("Error: $parameterName cannot be empty or blank", ExitCode.USAGE_ERROR)
        }
        return value
    }

    /**
     * Validates a list parameter and fails with appropriate error if empty.
     */
    protected fun <T> requireNotEmpty(list: List<T>, parameterName: String): List<T> {
        if (list.isEmpty()) {
            fail("Error: $parameterName cannot be empty", ExitCode.USAGE_ERROR)
        }
        return list
    }
}
