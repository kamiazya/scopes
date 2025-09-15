package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope uniqueness constraints.
 */
sealed class ScopeUniquenessError(recoverable: Boolean = true, cause: Throwable? = null) : ScopeManagementApplicationError(recoverable, cause) {
    data class DuplicateTitle(val title: String, val parentScopeId: String?, val existingScopeId: String) : ScopeUniquenessError()
}
