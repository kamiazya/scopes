package io.github.kamiazya.scopes.application.error

sealed class ScopeUniquenessError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data class DuplicateTitle(val title: String, val parentScopeId: String?, val existingScopeId: String) : ScopeUniquenessError()
}
