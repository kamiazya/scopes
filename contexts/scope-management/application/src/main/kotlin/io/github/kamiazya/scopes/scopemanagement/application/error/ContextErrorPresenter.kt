package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError

/**
 * Presenter for converting domain context errors to human-readable messages.
 * This service handles the presentation concern of error messages.
 */
class ContextErrorPresenter {

    fun presentInvalidFilter(errorType: ContextError.InvalidFilter.InvalidFilterType): String = when (errorType) {
        ContextError.InvalidFilter.InvalidFilterType.SYNTAX_ERROR -> "Filter has invalid syntax"
        ContextError.InvalidFilter.InvalidFilterType.UNKNOWN_OPERATOR -> "Filter contains unknown operator"
        ContextError.InvalidFilter.InvalidFilterType.INVALID_VALUE -> "Filter contains invalid value"
        ContextError.InvalidFilter.InvalidFilterType.MALFORMED_EXPRESSION -> "Filter expression is malformed"
        ContextError.InvalidFilter.InvalidFilterType.UNSUPPORTED_FILTER -> "Filter type is not supported"
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
