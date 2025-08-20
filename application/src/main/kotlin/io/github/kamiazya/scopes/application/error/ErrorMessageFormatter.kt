package io.github.kamiazya.scopes.application.error

/**
 * Service for formatting error information into user-readable messages.
 * Different presentation layers can provide their own implementations.
 */
interface ErrorMessageFormatter {
    /**
     * Format error information into a user-readable message.
     */
    fun format(errorInfo: ApplicationError): String
}
