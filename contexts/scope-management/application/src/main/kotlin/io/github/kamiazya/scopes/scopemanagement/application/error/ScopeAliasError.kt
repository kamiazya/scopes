package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope alias operations.
 */
sealed class ScopeAliasError(recoverable: Boolean = true, cause: Throwable? = null) : ScopeManagementApplicationError(recoverable, cause) {
    data class AliasDuplicate(val aliasName: String, val existingScopeId: String, val attemptedScopeId: String) : ScopeAliasError()
    data class AliasNotFound(val aliasName: String) : ScopeAliasError()
    data class CannotRemoveCanonicalAlias(val scopeId: String, val aliasName: String) : ScopeAliasError()
    data class AliasGenerationFailed(val scopeId: String, val retryCount: Int) : ScopeAliasError(recoverable = false)
    data class AliasGenerationValidationFailed(val scopeId: String, val reason: String, val attemptedValue: String) : ScopeAliasError()

    /**
     * Data inconsistency errors representing broken data relationships.
     * These are non-recoverable errors requiring manual intervention or data repair.
     */
    sealed class DataInconsistencyError : ScopeAliasError(recoverable = false) {
        /**
         * Alias exists but the referenced scope is not found.
         * This typically indicates a failed cascade delete or data corruption.
         */
        data class AliasExistsButScopeNotFound(val aliasName: String, val scopeId: String) : DataInconsistencyError()
    }
}
