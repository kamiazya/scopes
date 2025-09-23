package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Utility object containing shared error handling methods used by command and query handlers.
 */
object ErrorUtilityMethods {
    /**
     * Get error class name for consistent error logging.
     * Returns the qualified name if available, falls back to simple name,
     * or throws an error if neither is available.
     */
    fun getErrorClassName(error: ScopeContractError): String =
        error::class.qualifiedName ?: error::class.simpleName ?: error("Error class name must not be null")
}
