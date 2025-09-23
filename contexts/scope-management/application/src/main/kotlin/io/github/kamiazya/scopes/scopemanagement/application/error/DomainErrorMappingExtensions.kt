package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.application.util.InputSanitizer
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError as AppContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError.PersistenceError as AppPersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError as DomainPersistenceError

/**
 * Extension functions for mapping domain errors to application errors.
 * These mappings avoid the presenter anti-pattern by directly creating error messages.
 */

/**
 * Generic fallback for any ScopesError that doesn't have a specific mapping.
 * Use this sparingly - prefer context-specific mappings in handlers.
 */
fun ScopesError.toGenericApplicationError(): ScopeManagementApplicationError = when (this) {
    // Map hierarchy errors to appropriate application errors
    is ScopeHierarchyError.HierarchyUnavailable -> {
        AppPersistenceError.StorageUnavailable(
            operation = "hierarchy.${this.operation.name.lowercase()}",
        )
    }

    // Map other hierarchy errors to generic persistence errors
    is ScopeHierarchyError -> AppPersistenceError.StorageUnavailable(
        operation = "hierarchy",
    )

    // Map common domain errors to application errors
    is ScopesError.SystemError -> AppPersistenceError.StorageUnavailable(
        operation = this.context["operation"]?.toString() ?: "system-operation",
    )

    is ScopesError.NotFound -> AppPersistenceError.NotFound(
        entityType = this.entityType,
        entityId = this.identifier,
    )

    is ScopesError.ValidationFailed -> ScopeInputError.ValidationFailed(
        field = this.field,
        preview = InputSanitizer.createPreview(this.value),
        reason = "Validation failed: ${this.constraint}",
    )

    // For other errors, create a generic persistence error
    // This should be replaced with context-specific errors in actual handlers
    else -> AppPersistenceError.StorageUnavailable(
        operation = "domain-operation",
    )
}

/**
 * Maps ContextError to ApplicationError.ContextError.
 * Direct mapping without presenter dependencies.
 */
fun ContextError.toApplicationError(): ScopeManagementApplicationError = when (this) {
    is ContextError.KeyTooShort ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "key too short (minimum: ${this.minimumLength})",
        )

    is ContextError.KeyTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "key too long (maximum: ${this.maximumLength})",
        )

    is ContextError.InvalidKeyFormat ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = getInvalidKeyFormatMessage(this.errorType),
        )

    is ContextError.EmptyKey ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty key",
        )

    is ContextError.EmptyName ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty name",
        )

    is ContextError.NameTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "name too long (maximum: ${this.maximumLength})",
        )

    is ContextError.EmptyDescription ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty description",
        )

    is ContextError.DescriptionTooShort ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "description too short (minimum: ${this.minimumLength})",
        )

    is ContextError.DescriptionTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "description too long (maximum: ${this.maximumLength})",
        )

    is ContextError.EmptyFilter ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Empty filter",
        )

    is ContextError.FilterTooShort ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Filter too short (minimum: ${this.minimumLength})",
        )

    is ContextError.FilterTooLong ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Filter too long (maximum: ${this.maximumLength})",
        )

    is ContextError.InvalidFilterSyntax ->
        AppContextError.InvalidFilter(
            filter = this.expression,
            reason = getInvalidFilterSyntaxMessage(this.errorType),
        )

    is ContextError.InvalidScope ->
        AppContextError.StateNotFound(
            contextId = this.scopeId,
        )

    is ContextError.InvalidHierarchy ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = getInvalidHierarchyMessage(this.errorType),
        )

    is ContextError.DuplicateScope ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "Duplicate scope: ${this.title}",
        )
}

/**
 * Maps DomainPersistenceError to ApplicationError.PersistenceError.
 * Direct mapping without presenter dependencies.
 */
fun DomainPersistenceError.toApplicationError(): ScopeManagementApplicationError = when (this) {
    is DomainPersistenceError.ConcurrencyConflict ->
        AppPersistenceError.ConcurrencyConflict(
            entityType = this.entityType,
            entityId = this.entityId,
            expectedVersion = this.expectedVersion,
            actualVersion = this.actualVersion,
        )
}

/**
 * Helper functions for creating error messages without presenter dependencies.
 */
private fun getInvalidKeyFormatMessage(errorType: ContextError.InvalidKeyFormat.InvalidKeyFormatType): String = when (errorType) {
    ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_CHARACTERS -> "Key contains invalid characters"
    ContextError.InvalidKeyFormat.InvalidKeyFormatType.RESERVED_KEYWORD -> "Key is a reserved keyword"
    ContextError.InvalidKeyFormat.InvalidKeyFormatType.STARTS_WITH_NUMBER -> "Key cannot start with a number"
    ContextError.InvalidKeyFormat.InvalidKeyFormatType.CONTAINS_SPACES -> "Key cannot contain spaces"
    ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN ->
        "Key must start with a letter, contain only lowercase letters, numbers, hyphens, underscores, and not end with hyphen or underscore"
}

private fun getInvalidFilterSyntaxMessage(errorType: ContextError.FilterSyntaxErrorType): String = when (errorType) {
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

private fun getInvalidHierarchyMessage(errorType: ContextError.InvalidHierarchy.InvalidHierarchyType): String = when (errorType) {
    ContextError.InvalidHierarchy.InvalidHierarchyType.CIRCULAR_REFERENCE -> "Circular reference detected in hierarchy"
    ContextError.InvalidHierarchy.InvalidHierarchyType.DEPTH_LIMIT_EXCEEDED -> "Hierarchy depth limit exceeded"
    ContextError.InvalidHierarchy.InvalidHierarchyType.PARENT_NOT_FOUND -> "Parent scope not found"
    ContextError.InvalidHierarchy.InvalidHierarchyType.INVALID_PARENT_TYPE -> "Invalid parent type for scope"
    ContextError.InvalidHierarchy.InvalidHierarchyType.CROSS_CONTEXT_HIERARCHY -> "Cannot create hierarchy across different contexts"
}
