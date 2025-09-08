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

    data class InvalidFilter(override val occurredAt: Instant, val filter: String, val errorType: InvalidFilterType) : ContextError() {
        enum class InvalidFilterType {
            SYNTAX_ERROR,
            UNKNOWN_OPERATOR,
            INVALID_VALUE,
            MALFORMED_EXPRESSION,
            UNSUPPORTED_FILTER,
        }
    }

    // New error cases for ContextViewKey
    data class EmptyKey(override val occurredAt: Instant) : ContextError()

    data class KeyTooShort(val minimumLength: Int, override val occurredAt: Instant) : ContextError()

    data class KeyTooLong(val maximumLength: Int, override val occurredAt: Instant) : ContextError()

    data class InvalidKeyFormat(val errorType: InvalidKeyFormatType, override val occurredAt: Instant) : ContextError() {
        enum class InvalidKeyFormatType {
            INVALID_CHARACTERS,
            RESERVED_KEYWORD,
            STARTS_WITH_NUMBER,
            CONTAINS_SPACES,
            INVALID_PATTERN,
        }
    }

    // New error cases for ContextViewDescription
    data class EmptyDescription(override val occurredAt: Instant) : ContextError()

    data class DescriptionTooShort(val minimumLength: Int, override val occurredAt: Instant) : ContextError()

    data class DescriptionTooLong(val maximumLength: Int, override val occurredAt: Instant) : ContextError()

    // New error cases for ContextViewFilter
    data class EmptyFilter(override val occurredAt: Instant) : ContextError()

    data class FilterTooShort(val minimumLength: Int, override val occurredAt: Instant) : ContextError()

    data class FilterTooLong(val maximumLength: Int, override val occurredAt: Instant) : ContextError()

    /**
     * Represents a filter syntax validation error.
     *
     * @property expression The filter expression that failed validation
     * @property errorType The specific type of parsing error
     */
    data class InvalidFilterSyntax(val expression: String, val errorType: FilterSyntaxErrorType, override val occurredAt: Instant) : ContextError()

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
    data class InvalidScope(val scopeId: String, val errorType: InvalidScopeType, override val occurredAt: Instant) : ContextError() {
        enum class InvalidScopeType {
            SCOPE_NOT_FOUND,
            SCOPE_ARCHIVED,
            SCOPE_DELETED,
            INSUFFICIENT_PERMISSIONS,
            INVALID_STATE,
        }
    }

    data class InvalidHierarchy(val scopeId: String, val parentId: String, val errorType: InvalidHierarchyType, override val occurredAt: Instant) :
        ContextError() {
        enum class InvalidHierarchyType {
            CIRCULAR_REFERENCE,
            DEPTH_LIMIT_EXCEEDED,
            PARENT_NOT_FOUND,
            INVALID_PARENT_TYPE,
            CROSS_CONTEXT_HIERARCHY,
        }
    }

    data class DuplicateScope(val title: String, val contextId: String?, val errorType: DuplicateScopeType, override val occurredAt: Instant) : ContextError() {
        enum class DuplicateScopeType {
            TITLE_EXISTS_IN_CONTEXT,
            ALIAS_ALREADY_TAKEN,
            IDENTIFIER_CONFLICT,
        }
    }
}
