package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Base error for context management.
 */
sealed class ContextManagementError : ScopesError()

/**
 * Errors related to context view operations.
 */
sealed class ContextError : ContextManagementError() {

    // New error cases for ContextViewKey
    data object EmptyKey : ContextError()

    data class KeyTooShort(val minimumLength: Int) : ContextError()

    data class KeyTooLong(val maximumLength: Int) : ContextError()

    data class InvalidKeyFormat(val errorType: InvalidKeyFormatType) : ContextError() {
        enum class InvalidKeyFormatType {
            INVALID_CHARACTERS,
            RESERVED_KEYWORD,
            STARTS_WITH_NUMBER,
            CONTAINS_SPACES,
            INVALID_PATTERN,
        }
    }

    // New error cases for ContextViewName
    data object EmptyName : ContextError()

    data class NameTooLong(val maximumLength: Int) : ContextError()

    // New error cases for ContextViewDescription
    data object EmptyDescription : ContextError()

    data class DescriptionTooShort(val minimumLength: Int) : ContextError()

    data class DescriptionTooLong(val maximumLength: Int) : ContextError()

    // New error cases for ContextViewFilter
    data object EmptyFilter : ContextError()

    data class FilterTooShort(val minimumLength: Int) : ContextError()

    data class FilterTooLong(val maximumLength: Int) : ContextError()

    /**
     * Represents a filter syntax validation error.
     *
     * @property expression The filter expression that failed validation
     * @property errorType The specific type of parsing error
     */
    data class InvalidFilterSyntax(val expression: String, val errorType: FilterSyntaxErrorType) : ContextError()

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
    data class InvalidScope(val scopeId: String, val errorType: InvalidScopeType) : ContextError() {
        enum class InvalidScopeType {
            SCOPE_NOT_FOUND,
            SCOPE_ARCHIVED,
            SCOPE_DELETED,
            INSUFFICIENT_PERMISSIONS,
            INVALID_STATE,
        }
    }

    data class InvalidHierarchy(val scopeId: String, val parentId: String, val errorType: InvalidHierarchyType) : ContextError() {
        enum class InvalidHierarchyType {
            CIRCULAR_REFERENCE,
            DEPTH_LIMIT_EXCEEDED,
            PARENT_NOT_FOUND,
            INVALID_PARENT_TYPE,
            CROSS_CONTEXT_HIERARCHY,
        }
    }

    data class DuplicateScope(val title: String, val contextId: String?, val errorType: DuplicateScopeType) : ContextError() {
        enum class DuplicateScopeType {
            TITLE_EXISTS_IN_CONTEXT,
            ALIAS_ALREADY_TAKEN,
            IDENTIFIER_CONFLICT,
        }
    }
}
