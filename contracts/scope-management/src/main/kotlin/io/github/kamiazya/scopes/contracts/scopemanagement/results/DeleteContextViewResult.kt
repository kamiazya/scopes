package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of deleting a context view.
 */
public data class DeleteContextViewResult(
    /**
     * The key of the deleted context view.
     */
    public val deletedKey: String,

    /**
     * Whether this was the active context view.
     */
    public val wasActive: Boolean = false,
)
