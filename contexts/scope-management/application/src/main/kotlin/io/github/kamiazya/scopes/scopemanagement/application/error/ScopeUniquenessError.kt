package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope uniqueness constraints.
 *
 * This sealed class hierarchy represents all possible uniqueness violations
 * that can occur when creating or updating scopes. The system enforces
 * strict title uniqueness rules at all hierarchy levels.
 *
 * Design Principle: All scopes must have unique titles within their context
 * (whether root-level or within a parent scope). This ensures clear identification
 * and prevents ambiguity throughout the entire scope hierarchy.
 */
sealed class ScopeUniquenessError : ScopeManagementApplicationError() {
    /**
     * Indicates that a scope with the specified title already exists.
     *
     * This error is raised when attempting to create or update a scope
     * with a title that is already in use at the same hierarchy level.
     *
     * @property title The duplicate title that was attempted
     * @property parentScopeId The parent scope ID where uniqueness was violated (null for root level)
     * @property existingScopeId The ID of the existing scope that has this title
     */
    data class DuplicateTitle(val title: String, val parentScopeId: String?, val existingScopeId: String) : ScopeUniquenessError()
}
