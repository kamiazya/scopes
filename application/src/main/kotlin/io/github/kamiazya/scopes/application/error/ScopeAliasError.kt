package io.github.kamiazya.scopes.application.error

sealed class ScopeAliasError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data class DuplicateAlias(
        val aliasName: String,
        val existingScopeId: String,
        val attemptedScopeId: String
    ) : ScopeAliasError()

    data class AliasNotFound(
        val aliasName: String
    ) : ScopeAliasError()

    data class CannotRemoveCanonicalAlias(
        val scopeId: String,
        val aliasName: String
    ) : ScopeAliasError()

    data class AliasGenerationFailed(
        val scopeId: String,
        val retryCount: Int
    ) : ScopeAliasError()
}