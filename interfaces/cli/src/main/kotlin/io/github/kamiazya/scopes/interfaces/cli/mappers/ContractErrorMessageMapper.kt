package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError

/**
 * Maps contract errors to user-friendly error messages for CLI output
 */
object ContractErrorMessageMapper {
    /**
     * Gets error message with optional debug information
     */
    fun getMessage(error: ScopeContractError, debug: Boolean = false): String = when (error) {
        is ScopeContractError.InputError.InvalidId -> {
            val formatHint = error.expectedFormat?.let { " (expected: $it)" } ?: ""
            if (debug) {
                "Invalid scope ID format: '${error.id}'$formatHint [provided value: ${error.id}]"
            } else {
                "Invalid scope ID format: ${error.id}$formatHint"
            }
        }
        is ScopeContractError.InputError.InvalidTitle -> when (val failure = error.validationFailure) {
            is ScopeContractError.TitleValidationFailure.Empty -> "Title cannot be empty"
            is ScopeContractError.TitleValidationFailure.TooShort ->
                "Title is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
            is ScopeContractError.TitleValidationFailure.TooLong ->
                "Title is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
            is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
                "Title contains prohibited characters: ${failure.prohibitedCharacters.joinToString(", ")}"
        }
        is ScopeContractError.InputError.InvalidDescription -> when (val failure = error.validationFailure) {
            is ScopeContractError.DescriptionValidationFailure.TooLong ->
                "Description is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        }
        is ScopeContractError.InputError.InvalidParentId -> {
            val formatHint = error.expectedFormat?.let { " (expected: $it)" } ?: ""
            "Invalid parent scope ID: ${error.parentId}$formatHint"
        }

        is ScopeContractError.BusinessError.NotFound -> {
            if (debug) {
                "Scope not found: '${error.scopeId}' [ULID/alias searched: ${error.scopeId}]"
            } else {
                "Scope not found: ${error.scopeId}"
            }
        }
        is ScopeContractError.BusinessError.DuplicateTitle -> {
            val location = error.parentId?.let { "under parent $it" } ?: "at root level"
            if (debug) {
                "A scope with title '${error.title}' already exists $location [parent ULID: ${error.parentId ?: "root"}]"
            } else {
                "A scope with title '${error.title}' already exists $location"
            }
        }
        is ScopeContractError.BusinessError.HierarchyViolation -> when (val violation = error.violation) {
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
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Scope is already deleted: ${error.scopeId}"
        is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
        is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
        is ScopeContractError.BusinessError.HasChildren -> {
            val countInfo = error.childrenCount?.let { " (has $it children)" } ?: ""
            "Cannot delete scope with children: ${error.scopeId}$countInfo"
        }
        is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateAlias -> "Alias already exists: ${error.alias}"
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias ->
            "Cannot remove canonical alias. Set a different alias as canonical first."

        is ScopeContractError.SystemError.ServiceUnavailable -> "Service unavailable: ${error.service}"
        is ScopeContractError.SystemError.Timeout -> "Operation timed out: ${error.operation}"
        is ScopeContractError.SystemError.ConcurrentModification -> "Concurrent modification detected. Please retry the operation."
    }

    /**
     * Gets error message for user preferences errors with optional debug information
     */
    fun getMessage(error: UserPreferencesContractError, debug: Boolean = false): String = when (error) {
        is UserPreferencesContractError.InputError.InvalidPreferenceKey -> "Invalid preference key: ${error.key}"

        is UserPreferencesContractError.DataError.PreferencesCorrupted -> "Preferences data is corrupted (see logs for details)"
        is UserPreferencesContractError.DataError.PreferencesMigrationRequired ->
            "Preferences migration required from version ${error.fromVersion} to ${error.toVersion}"
    }
}
