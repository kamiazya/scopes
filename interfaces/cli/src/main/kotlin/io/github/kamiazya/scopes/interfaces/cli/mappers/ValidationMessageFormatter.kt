package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Shared formatter for validation failure messages to reduce code duplication
 * between different error mappers.
 */
internal object ValidationMessageFormatter {
    
    fun formatTitleValidationFailure(failure: ScopeContractError.TitleValidationFailure): String = when (failure) {
        is ScopeContractError.TitleValidationFailure.Empty -> "Title cannot be empty"
        is ScopeContractError.TitleValidationFailure.TooShort ->
            "Title is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.TitleValidationFailure.TooLong ->
            "Title is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
            "Title contains prohibited characters: ${failure.prohibitedCharacters.joinToString(", ")}"
    }

    fun formatDescriptionValidationFailure(failure: ScopeContractError.DescriptionValidationFailure): String = when (failure) {
        is ScopeContractError.DescriptionValidationFailure.TooLong ->
            "Description is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
    }

    fun formatAliasValidationFailure(failure: ScopeContractError.AliasValidationFailure): String = when (failure) {
        is ScopeContractError.AliasValidationFailure.Empty -> "Alias cannot be empty"
        is ScopeContractError.AliasValidationFailure.TooShort ->
            "Alias is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.AliasValidationFailure.TooLong ->
            "Alias is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.AliasValidationFailure.InvalidFormat ->
            "Invalid alias format (expected: ${failure.expectedPattern})"
    }

    fun formatContextKeyValidationFailure(failure: ScopeContractError.ContextKeyValidationFailure): String = when (failure) {
        is ScopeContractError.ContextKeyValidationFailure.Empty -> "Context key cannot be empty"
        is ScopeContractError.ContextKeyValidationFailure.TooShort ->
            "Context key is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextKeyValidationFailure.TooLong ->
            "Context key is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextKeyValidationFailure.InvalidFormat ->
            "Invalid context key format: ${failure.invalidType}"
    }

    fun formatContextNameValidationFailure(failure: ScopeContractError.ContextNameValidationFailure): String = when (failure) {
        is ScopeContractError.ContextNameValidationFailure.Empty -> "Context name cannot be empty"
        is ScopeContractError.ContextNameValidationFailure.TooLong ->
            "Context name is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
    }

    fun formatContextFilterValidationFailure(failure: ScopeContractError.ContextFilterValidationFailure): String = when (failure) {
        is ScopeContractError.ContextFilterValidationFailure.Empty -> "Context filter cannot be empty"
        is ScopeContractError.ContextFilterValidationFailure.TooShort ->
            "Context filter is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextFilterValidationFailure.TooLong ->
            "Context filter is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextFilterValidationFailure.InvalidSyntax -> {
            val position = failure.position?.let { " at position $it" } ?: ""
            "Invalid filter syntax: ${failure.errorType}$position"
        }
    }

    fun formatHierarchyViolation(violation: ScopeContractError.HierarchyViolationType): String = when (violation) {
        is ScopeContractError.HierarchyViolationType.CircularReference -> {
            val pathInfo = violation.cyclePath?.let { path ->
                " (cycle: ${path.joinToString(" -> ")})"
            } ?: ""
            "Circular reference detected: scope ${violation.scopeId} cannot have parent ${violation.parentId}$pathInfo"
        }
        is ScopeContractError.HierarchyViolationType.MaxDepthExceeded ->
            "Maximum hierarchy depth exceeded: attempted ${violation.attemptedDepth}, maximum ${violation.maximumDepth}"
        is ScopeContractError.HierarchyViolationType.MaxChildrenExceeded ->
            "Maximum children exceeded for parent ${violation.parentId}: current ${violation.currentChildrenCount}, maximum ${violation.maximumChildren}"
        is ScopeContractError.HierarchyViolationType.SelfParenting ->
            "Cannot set scope ${violation.scopeId} as its own parent"
        is ScopeContractError.HierarchyViolationType.ParentNotFound ->
            "Parent scope ${violation.parentId} not found for scope ${violation.scopeId}"
    }
}