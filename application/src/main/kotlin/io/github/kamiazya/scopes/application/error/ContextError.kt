package io.github.kamiazya.scopes.application.error

sealed class ContextError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    // Key-related errors
    data object KeyEmpty : ContextError()
    data class KeyAlreadyExists(val attemptedKey: String) : ContextError()
    data class KeyInvalidFormat(val attemptedKey: String) : ContextError()

    // Name-related errors
    data object NameEmpty : ContextError()
    data class NameTooLong(val attemptedName: String, val maximumLength: Int) : ContextError()
    data class NameInvalidFormat(val attemptedName: String) : ContextError()

    data class FilterInvalidSyntax(val position: Int, val reason: String, val expression: String) : ContextError()
    data class FilterUnknownAspect(val unknownAspectKey: String, val expression: String) : ContextError()
    data class FilterLogicalInconsistency(val reason: String, val expression: String) : ContextError()

    data class StateNotFound(val contextId: String) : ContextError()
    data class StateFilterProducesNoResults(val contextName: String, val filterExpression: String) : ContextError()
    data class ActiveContextDeleteAttempt(val contextId: String) : ContextError()
}
