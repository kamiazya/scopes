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
            val formatHint = error.expectedFormat?.run { " (expected: $this)" } ?: ""
            if (debug) {
                "Invalid scope ID format: '${error.id}'$formatHint [provided value: ${error.id}]"
            } else {
                "Invalid scope ID format: ${error.id}$formatHint"
            }
        }
        is ScopeContractError.InputError.InvalidTitle -> ValidationMessageFormatter.formatTitleValidationFailure(error.validationFailure)
        is ScopeContractError.InputError.InvalidDescription -> ValidationMessageFormatter.formatDescriptionValidationFailure(error.validationFailure)
        is ScopeContractError.InputError.InvalidParentId -> {
            val formatHint = error.expectedFormat?.run { " (expected: $this)" } ?: ""
            "Invalid parent scope ID: ${error.parentId}$formatHint"
        }
        is ScopeContractError.InputError.InvalidAlias -> ValidationMessageFormatter.formatAliasValidationFailure(error.validationFailure)
        is ScopeContractError.InputError.InvalidContextKey -> ValidationMessageFormatter.formatContextKeyValidationFailure(error.validationFailure)
        is ScopeContractError.InputError.InvalidContextName -> ValidationMessageFormatter.formatContextNameValidationFailure(error.validationFailure)
        is ScopeContractError.InputError.InvalidContextFilter -> {
            val formatted = ValidationMessageFormatter.formatContextFilterValidationFailure(error.validationFailure)
            // Add expression context for InvalidSyntax case
            val failure = error.validationFailure
            if (failure is ScopeContractError.ContextFilterValidationFailure.InvalidSyntax) {
                "Invalid filter syntax in '${failure.expression}': ${failure.errorType}${failure.position?.run { " at position $this" } ?: ""}"
            } else {
                formatted
            }
        }

        is ScopeContractError.BusinessError.NotFound -> {
            if (debug) {
                "Scope not found: '${error.scopeId}' [ULID/alias searched: ${error.scopeId}]"
            } else {
                "Scope not found: ${error.scopeId}"
            }
        }
        is ScopeContractError.BusinessError.DuplicateTitle -> {
            val location = error.parentId?.run { "under parent $this" } ?: "at root level"
            if (debug) {
                "A scope with title '${error.title}' already exists $location [parent ULID: ${error.parentId ?: "root"}]"
            } else {
                "A scope with title '${error.title}' already exists $location"
            }
        }
        is ScopeContractError.BusinessError.HierarchyViolation -> ValidationMessageFormatter.formatHierarchyViolation(error.violation)
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Scope is already deleted: ${error.scopeId}"
        is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
        is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
        is ScopeContractError.BusinessError.HasChildren -> {
            val countInfo = error.childrenCount?.run { " (has $this children)" } ?: ""
            "Cannot delete scope with children: ${error.scopeId}$countInfo"
        }
        is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateAlias -> "Alias already exists: ${error.alias}"
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias ->
            "Cannot remove canonical alias. Set a different alias as canonical first."
        is ScopeContractError.BusinessError.AliasOfDifferentScope ->
            "Alias '${error.alias}' belongs to a different scope (expected: ${error.expectedScopeId}, actual: ${error.actualScopeId})"
        is ScopeContractError.BusinessError.AliasGenerationFailed ->
            "Failed to generate alias for scope ${error.scopeId} after ${error.retryCount} retries"
        is ScopeContractError.BusinessError.AliasGenerationValidationFailed ->
            "Generated alias '${error.alias}' failed validation: ${error.reason}"
        is ScopeContractError.BusinessError.ContextNotFound -> "Context view not found: ${error.contextKey}"
        is ScopeContractError.BusinessError.DuplicateContextKey ->
            "Context key already exists: ${error.contextKey}"

        is ScopeContractError.DataInconsistency.MissingCanonicalAlias ->
            "Data inconsistency detected: Scope ${error.scopeId} is missing its canonical alias. This indicates data corruption. Contact administrator to rebuild aliases."

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
