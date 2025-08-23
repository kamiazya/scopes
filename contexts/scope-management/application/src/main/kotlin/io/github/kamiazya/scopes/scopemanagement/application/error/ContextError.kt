package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to context view operations.
 */
sealed class ContextError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data class KeyInvalidFormat(val attemptedKey: String) : ContextError()

    data class StateNotFound(val contextId: String) : ContextError()

    data class InvalidFilter(val filter: String, val reason: String) : ContextError()
}
