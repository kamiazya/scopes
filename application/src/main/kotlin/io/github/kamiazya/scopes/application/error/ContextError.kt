package io.github.kamiazya.scopes.application.error

sealed class ContextError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data object NamingEmpty : ContextError()
    data class NamingAlreadyExists(val attemptedName: String) : ContextError()
    data class NamingInvalidFormat(val attemptedName: String) : ContextError()

    data class FilterInvalidSyntax(
        val position: Int,
        val reason: String,
        val expression: String
    ) : ContextError()
    data class FilterUnknownAspect(
        val unknownAspectKey: String,
        val expression: String
    ) : ContextError()
    data class FilterLogicalInconsistency(
        val reason: String,
        val expression: String
    ) : ContextError()

    data class StateNotFound(
        val contextName: String? = null,
        val contextId: String? = null
    ) : ContextError()
    data class StateFilterProducesNoResults(
        val contextName: String,
        val filterExpression: String
    ) : ContextError()
    data class ActiveContextDeleteAttempt(
        val contextId: String
    ) : ContextError()
}
