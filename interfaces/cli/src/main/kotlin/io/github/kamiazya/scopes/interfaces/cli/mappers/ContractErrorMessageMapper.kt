package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError

/**
 * Maps contract errors to user-friendly error messages for CLI output
 */
object ContractErrorMessageMapper {
    fun getMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.InputError.InvalidId -> "Invalid scope ID format: ${error.id}"
        is ScopeContractError.InputError.InvalidTitle -> "Invalid title: ${error.reason}"
        is ScopeContractError.InputError.InvalidDescription -> "Invalid description: ${error.reason}"
        is ScopeContractError.InputError.InvalidParentId -> "Invalid parent scope ID: ${error.parentId}"

        is ScopeContractError.BusinessError.NotFound -> "Scope not found: ${error.scopeId}"
        is ScopeContractError.BusinessError.DuplicateTitle -> "A scope with title '${error.title}' already exists under ${error.parentId ?: "root"}"
        is ScopeContractError.BusinessError.HierarchyViolation -> "Hierarchy violation: ${error.reason}"
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Scope is already deleted: ${error.scopeId}"
        is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
        is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
        is ScopeContractError.BusinessError.HasChildren -> "Cannot delete scope with children: ${error.scopeId}"

        is ScopeContractError.SystemError.ServiceUnavailable -> "Service unavailable: ${error.service}"
        is ScopeContractError.SystemError.Timeout -> "Operation timed out: ${error.operation}"
        is ScopeContractError.SystemError.ConcurrentModification -> "Concurrent modification detected. Please retry the operation."
    }

    fun getMessage(error: UserPreferencesContractError): String = when (error) {
        is UserPreferencesContractError.InputError.InvalidPreferenceKey -> "Invalid preference key: ${error.key}"

        is UserPreferencesContractError.DataError.PreferencesCorrupted -> "Preferences data is corrupted (see logs for details)"
        is UserPreferencesContractError.DataError.PreferencesMigrationRequired ->
            "Preferences migration required from version ${error.fromVersion} to ${error.toVersion}"

        is UserPreferencesContractError.SystemError.PreferencesReadError -> "Failed to read preferences (see logs for details)"
        is UserPreferencesContractError.SystemError.PreferencesWriteError -> "Failed to write preferences (see logs for details)"
        is UserPreferencesContractError.SystemError.ServiceUnavailable -> "User preferences service unavailable: ${error.service}"
    }
}
