package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Base error for context management.
 */
sealed class ContextManagementError : ScopesError()

/**
 * Errors related to context view operations.
 */
sealed class ContextError : ContextManagementError() {

    data class BlankId(override val occurredAt: Instant, val attemptedValue: String) : ContextError()

    data class InvalidIdFormat(override val occurredAt: Instant, val attemptedValue: String, val expectedFormat: String) : ContextError()

    data class EmptyName(override val occurredAt: Instant, val attemptedValue: String) : ContextError()

    data class InvalidNameFormat(override val occurredAt: Instant, val attemptedValue: String, val expectedPattern: String) : ContextError()

    data class NameTooLong(override val occurredAt: Instant, val attemptedValue: String, val maximumLength: Int) : ContextError()

    data class DuplicateName(override val occurredAt: Instant, val attemptedName: String, val existingContextId: String) : ContextError()

    data class ContextNotFound(override val occurredAt: Instant, val contextId: String? = null, val contextName: String? = null) : ContextError()

    data class InvalidFilter(override val occurredAt: Instant, val filter: String, val reason: String) : ContextError()

    // New error cases for ContextViewKey
    data object EmptyKey : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class KeyTooShort(val minimumLength: Int) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class KeyTooLong(val maximumLength: Int) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class InvalidKeyFormat(val reason: String) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    // New error cases for ContextViewDescription
    data object EmptyDescription : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class DescriptionTooShort(val minimumLength: Int) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class DescriptionTooLong(val maximumLength: Int) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    // New error cases for ContextViewFilter
    data object EmptyFilter : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class FilterTooShort(val minimumLength: Int) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class FilterTooLong(val maximumLength: Int) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    /**
     * Represents a filter syntax validation error.
     *
     * @property expression The filter expression that failed validation
     * @property errorType The specific type of parsing error
     */
    data class InvalidFilterSyntax(val expression: String, val errorType: FilterSyntaxErrorType) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    /**
     * Types of filter syntax errors.
     * This sealed class provides type-safe error categorization
     * while preserving technical details for proper error handling.
     */
    sealed class FilterSyntaxErrorType {
        data object EmptyQuery : FilterSyntaxErrorType()
        data object EmptyExpression : FilterSyntaxErrorType()
        data class UnexpectedCharacter(val char: Char, val position: Int) : FilterSyntaxErrorType()
        data class UnterminatedString(val position: Int) : FilterSyntaxErrorType()
        data class UnexpectedToken(val position: Int) : FilterSyntaxErrorType()
        data class MissingClosingParen(val position: Int) : FilterSyntaxErrorType()
        data class ExpectedExpression(val position: Int) : FilterSyntaxErrorType()
        data class ExpectedIdentifier(val position: Int) : FilterSyntaxErrorType()
        data class ExpectedOperator(val position: Int) : FilterSyntaxErrorType()
        data class ExpectedValue(val position: Int) : FilterSyntaxErrorType()
        data object UnbalancedParentheses : FilterSyntaxErrorType()
        data object UnbalancedQuotes : FilterSyntaxErrorType()
        data object EmptyOperator : FilterSyntaxErrorType()
        data object InvalidSyntax : FilterSyntaxErrorType()
    }

    // Scope hierarchy validation errors
    data class InvalidScope(val scopeId: String, val reason: String) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class InvalidHierarchy(val scopeId: String, val parentId: String, val reason: String) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }

    data class DuplicateScope(val title: String, val contextId: String?, val reason: String) : ContextError() {
        override val occurredAt: Instant = kotlinx.datetime.Clock.System.now()
    }
}
