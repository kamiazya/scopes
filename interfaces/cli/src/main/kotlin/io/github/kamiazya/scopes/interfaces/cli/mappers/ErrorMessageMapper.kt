package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Maps domain and contract errors to user-friendly messages for CLI output.
 */
object ErrorMessageMapper {
    /**
     * Maps any error to a user-friendly message.
     */
    fun toUserMessage(error: Any): String = when (error) {
        is ScopeContractError -> getMessage(error)
        else -> when {
            error.toString().contains("NotFound") -> "The requested item was not found"
            error.toString().contains("AlreadyExists") -> "The item already exists"
            error.toString().contains("Invalid") -> "Invalid input provided"
            error.toString().contains("Conflict") -> "Operation conflicts with current state"
            error.toString().contains("Unavailable") -> "Service temporarily unavailable"
            else -> "An error occurred: $error"
        }
    }

    /**
     * Maps contract errors to user-friendly messages.
     */
    fun getMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.InputError -> when (error) {
            is ScopeContractError.InputError.InvalidId ->
                "Invalid ID format: ${error.id}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"
            is ScopeContractError.InputError.InvalidTitle -> when (val failure = error.validationFailure) {
                is ScopeContractError.TitleValidationFailure.Empty -> "Title cannot be empty"
                is ScopeContractError.TitleValidationFailure.TooShort ->
                    "Title too short: minimum ${failure.minimumLength} characters"
                is ScopeContractError.TitleValidationFailure.TooLong ->
                    "Title too long: maximum ${failure.maximumLength} characters"
                is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
                    "Title contains invalid characters: ${failure.prohibitedCharacters.joinToString()}"
            }
            is ScopeContractError.InputError.InvalidDescription -> when (val failure = error.validationFailure) {
                is ScopeContractError.DescriptionValidationFailure.TooLong ->
                    "Description too long: maximum ${failure.maximumLength} characters"
            }
            is ScopeContractError.InputError.InvalidParentId ->
                "Invalid parent ID: ${error.parentId}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"
            is ScopeContractError.InputError.InvalidAlias -> when (val failure = error.validationFailure) {
                is ScopeContractError.AliasValidationFailure.Empty -> "Alias cannot be empty"
                is ScopeContractError.AliasValidationFailure.TooShort ->
                    "Alias too short: minimum ${failure.minimumLength} characters"
                is ScopeContractError.AliasValidationFailure.TooLong ->
                    "Alias too long: maximum ${failure.maximumLength} characters"
                is ScopeContractError.AliasValidationFailure.InvalidFormat ->
                    "Invalid alias format (expected: ${failure.expectedPattern})"
            }
            is ScopeContractError.InputError.InvalidContextKey -> when (val failure = error.validationFailure) {
                is ScopeContractError.ContextKeyValidationFailure.Empty -> "Context key cannot be empty"
                is ScopeContractError.ContextKeyValidationFailure.TooShort ->
                    "Context key too short: minimum ${failure.minimumLength} characters"
                is ScopeContractError.ContextKeyValidationFailure.TooLong ->
                    "Context key too long: maximum ${failure.maximumLength} characters"
                is ScopeContractError.ContextKeyValidationFailure.InvalidFormat ->
                    "Invalid context key format: ${failure.invalidType}"
            }
            is ScopeContractError.InputError.InvalidContextName -> when (val failure = error.validationFailure) {
                is ScopeContractError.ContextNameValidationFailure.Empty -> "Context name cannot be empty"
                is ScopeContractError.ContextNameValidationFailure.TooLong ->
                    "Context name too long: maximum ${failure.maximumLength} characters"
            }
            is ScopeContractError.InputError.InvalidContextFilter -> when (val failure = error.validationFailure) {
                is ScopeContractError.ContextFilterValidationFailure.Empty -> "Context filter cannot be empty"
                is ScopeContractError.ContextFilterValidationFailure.TooShort ->
                    "Context filter too short: minimum ${failure.minimumLength} characters"
                is ScopeContractError.ContextFilterValidationFailure.TooLong ->
                    "Context filter too long: maximum ${failure.maximumLength} characters"
                is ScopeContractError.ContextFilterValidationFailure.InvalidSyntax -> {
                    val position = failure.position?.let { " at position $it" } ?: ""
                    "Invalid filter syntax: ${failure.errorType}$position"
                }
            }
        }
        is ScopeContractError.BusinessError -> when (error) {
            is ScopeContractError.BusinessError.NotFound -> "Not found: ${error.scopeId}"
            is ScopeContractError.BusinessError.DuplicateTitle ->
                "Duplicate title '${error.title}'${error.parentId?.let { " under parent $it" } ?: " at root level"}"
            is ScopeContractError.BusinessError.HierarchyViolation -> when (val violation = error.violation) {
                is ScopeContractError.HierarchyViolationType.CircularReference ->
                    "Circular reference detected: ${violation.scopeId} -> ${violation.parentId}"
                is ScopeContractError.HierarchyViolationType.MaxDepthExceeded ->
                    "Maximum depth exceeded: ${violation.attemptedDepth} (max: ${violation.maximumDepth})"
                is ScopeContractError.HierarchyViolationType.MaxChildrenExceeded ->
                    "Maximum children exceeded for ${violation.parentId}: ${violation.currentChildrenCount} (max: ${violation.maximumChildren})"
                is ScopeContractError.HierarchyViolationType.SelfParenting ->
                    "Cannot set scope ${violation.scopeId} as its own parent"
                is ScopeContractError.HierarchyViolationType.ParentNotFound ->
                    "Parent ${violation.parentId} not found for scope ${violation.scopeId}"
            }
            is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted: ${error.scopeId}"
            is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
            is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
            is ScopeContractError.BusinessError.HasChildren ->
                "Cannot delete scope with children: ${error.scopeId}${error.childrenCount?.let { " ($it children)" } ?: ""}"
            is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
            is ScopeContractError.BusinessError.DuplicateAlias -> "Alias already exists: ${error.alias}"
            is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias ->
                "Cannot remove canonical alias"
            is ScopeContractError.BusinessError.AliasOfDifferentScope ->
                "Alias '${error.alias}' belongs to different scope"
            is ScopeContractError.BusinessError.AliasGenerationFailed ->
                "Failed to generate alias (retries: ${error.retryCount})"
            is ScopeContractError.BusinessError.AliasGenerationValidationFailed ->
                "Generated alias invalid: ${error.reason}"
            is ScopeContractError.BusinessError.ContextNotFound -> "Context not found: ${error.contextKey}"
            is ScopeContractError.BusinessError.DuplicateContextKey -> "Context key already exists: ${error.contextKey}"
        }
        is ScopeContractError.DataInconsistency.MissingCanonicalAlias ->
            "Data inconsistency: Scope ${error.scopeId} is missing its canonical alias"
        is ScopeContractError.SystemError -> when (error) {
            is ScopeContractError.SystemError.ServiceUnavailable ->
                "Service unavailable: ${error.service}"
            is ScopeContractError.SystemError.Timeout ->
                "Operation timeout: ${error.operation} (${error.timeout})"
            is ScopeContractError.SystemError.ConcurrentModification ->
                "Concurrent modification detected for ${error.scopeId} (expected: ${error.expectedVersion}, actual: ${error.actualVersion})"
        }
    }
}
