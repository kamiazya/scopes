package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope uniqueness constraints.
 */
sealed class ScopeUniquenessError : ScopeManagementApplicationError() {
    data class DuplicateTitle(val title: String, val parentScopeId: String?, val existingScopeId: String) : ScopeUniquenessError()
}
