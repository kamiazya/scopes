package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of deleting a scope.
 */
public data class DeleteScopeResult(
    /**
     * The ID of the deleted scope.
     */
    public val deletedScopeId: String,

    /**
     * Number of child scopes deleted (if cascade was true).
     */
    public val deletedChildrenCount: Int = 0,
)
