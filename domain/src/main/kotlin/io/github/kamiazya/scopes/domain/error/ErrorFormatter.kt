package io.github.kamiazya.scopes.domain.error

import arrow.core.NonEmptyList

/**
 * Port interface for error formatting operations.
 *
 * This interface defines the contract for formatting domain errors into user-friendly messages.
 * Implementations should be provided by the infrastructure layer (adapter pattern).
 */
interface ErrorFormatter {
    /**
     * Formats a single domain error into a user-friendly message.
     */
    fun getErrorMessage(error: DomainError): String

    /**
     * Formats a collection of errors into a user-friendly summary message.
     */
    fun formatErrorSummary(errors: NonEmptyList<DomainError>): String

    /**
     * Formats validation error with specific field context.
     */
    fun getValidationErrorMessage(error: DomainError.ScopeValidationError): String

    /**
     * Formats business rule violation with business context.
     */
    fun getBusinessRuleViolationMessage(error: DomainError.ScopeBusinessRuleViolation): String

    /**
     * Formats repository error with technical details.
     */
    fun getRepositoryErrorMessage(error: RepositoryError): String
}
