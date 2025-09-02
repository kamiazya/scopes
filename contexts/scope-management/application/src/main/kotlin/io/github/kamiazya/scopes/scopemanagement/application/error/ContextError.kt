package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to context view operations.
 */
sealed class ContextError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data class KeyInvalidFormat(val attemptedKey: String) : ContextError()

    data class StateNotFound(val contextId: String) : ContextError()

    data class InvalidFilter(val filter: String, val reason: String) : ContextError()

    /**
     * Error when trying to delete or modify a context that is currently active
     */
    data class ContextInUse(val key: String) : ContextError(recoverable = false)

    /**
     * Error when trying to create a context with a key that already exists
     */
    data class DuplicateContextKey(val key: String) : ContextError()

    /**
     * Error when the requested context is not found
     */
    data class ContextNotFound(val key: String) : ContextError()

    /**
     * Error when there's a conflict during context update
     */
    data class ContextUpdateConflict(val key: String, val reason: String) : ContextError()

    /**
     * Error when switching to a non-existent context
     */
    data class InvalidContextSwitch(val key: String) : ContextError()
}
