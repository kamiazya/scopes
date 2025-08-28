package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope alias operations.
 */
sealed class ScopeAliasError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data class AliasDuplicate(val aliasName: String, val existingScopeId: String, val attemptedScopeId: String) : ScopeAliasError()
    data class AliasNotFound(val aliasName: String) : ScopeAliasError()
    data class CannotRemoveCanonicalAlias(val scopeId: String, val aliasName: String) : ScopeAliasError()
    data class AliasGenerationFailed(val scopeId: String, val retryCount: Int) : ScopeAliasError(recoverable = false)
    data class AliasGenerationValidationFailed(val scopeId: String, val reason: String, val attemptedValue: String) : ScopeAliasError()
    data class DataInconsistency(val message: String, val aliasName: String, val scopeId: String) : ScopeAliasError(recoverable = false)
}
