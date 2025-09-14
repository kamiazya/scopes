package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Errors related to scope uniqueness violations.
 */
sealed class ScopeUniquenessError : ScopesError() {

    /**
     * Error when a scope with the same title already exists at the same level.
     */
    data class DuplicateTitleInContext(val title: String, val parentId: ScopeId?, val existingId: ScopeId) : ScopeUniquenessError()

    /**
     * Error when a duplicate identifier is encountered.
     */
    data class DuplicateIdentifier(val identifier: String, val identifierType: String) : ScopeUniquenessError()
}
