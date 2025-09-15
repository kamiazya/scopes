package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError

/**
 * Presenter for converting domain context errors to human-readable messages.
 * This service handles the presentation concern of error messages.
 */
class ContextErrorPresenter {

    fun presentInvalidFilterSyntax(errorType: ContextError.FilterSyntaxErrorType): String = when (errorType) {
        ContextError.FilterSyntaxErrorType.EmptyQuery -> "Filter query is empty"
        ContextError.FilterSyntaxErrorType.EmptyExpression -> "Filter expression is empty"
        is ContextError.FilterSyntaxErrorType.UnexpectedCharacter -> "Unexpected character '${errorType.char}' at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.UnterminatedString -> "Unterminated string at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.UnexpectedToken -> "Unexpected token at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.MissingClosingParen -> "Missing closing parenthesis at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedExpression -> "Expected expression at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedIdentifier -> "Expected identifier at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedOperator -> "Expected operator at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedValue -> "Expected value at position ${errorType.position}"
        ContextError.FilterSyntaxErrorType.UnbalancedParentheses -> "Unbalanced parentheses in filter"
        ContextError.FilterSyntaxErrorType.UnbalancedQuotes -> "Unbalanced quotes in filter"
        ContextError.FilterSyntaxErrorType.EmptyOperator -> "Empty operator in filter"
        ContextError.FilterSyntaxErrorType.InvalidSyntax -> "Invalid filter syntax"
    }

    fun presentInvalidKeyFormat(errorType: ContextError.InvalidKeyFormat.InvalidKeyFormatType): String = when (errorType) {
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_CHARACTERS -> "Key contains invalid characters"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.RESERVED_KEYWORD -> "Key is a reserved keyword"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.STARTS_WITH_NUMBER -> "Key cannot start with a number"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.CONTAINS_SPACES -> "Key cannot contain spaces"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN ->
            "Key must start with a letter, contain only lowercase letters, numbers, hyphens, underscores, and not end with hyphen or underscore"
    }

    fun presentInvalidScope(errorType: ContextError.InvalidScope.InvalidScopeType): String = when (errorType) {
        ContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND -> "Scope not found"
        ContextError.InvalidScope.InvalidScopeType.SCOPE_ARCHIVED -> "Scope is archived"
        ContextError.InvalidScope.InvalidScopeType.SCOPE_DELETED -> "Scope is deleted"
        ContextError.InvalidScope.InvalidScopeType.INSUFFICIENT_PERMISSIONS -> "Insufficient permissions to access scope"
        ContextError.InvalidScope.InvalidScopeType.INVALID_STATE -> "Scope is in invalid state"
    }

    fun presentInvalidHierarchy(errorType: ContextError.InvalidHierarchy.InvalidHierarchyType): String = when (errorType) {
        ContextError.InvalidHierarchy.InvalidHierarchyType.CIRCULAR_REFERENCE -> "Circular reference detected in hierarchy"
        ContextError.InvalidHierarchy.InvalidHierarchyType.DEPTH_LIMIT_EXCEEDED -> "Hierarchy depth limit exceeded"
        ContextError.InvalidHierarchy.InvalidHierarchyType.PARENT_NOT_FOUND -> "Parent scope not found"
        ContextError.InvalidHierarchy.InvalidHierarchyType.INVALID_PARENT_TYPE -> "Invalid parent type for scope"
        ContextError.InvalidHierarchy.InvalidHierarchyType.CROSS_CONTEXT_HIERARCHY -> "Cannot create hierarchy across different contexts"
    }

    fun presentDuplicateScope(errorType: ContextError.DuplicateScope.DuplicateScopeType): String = when (errorType) {
        ContextError.DuplicateScope.DuplicateScopeType.TITLE_EXISTS_IN_CONTEXT -> "Scope with this title already exists in the context"
        ContextError.DuplicateScope.DuplicateScopeType.ALIAS_ALREADY_TAKEN -> "Alias is already taken by another scope"
        ContextError.DuplicateScope.DuplicateScopeType.IDENTIFIER_CONFLICT -> "Scope identifier conflict"
    }
}
