package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result containing a list of context views.
 */
public data class ContextViewListResult(
    /**
     * The list of context views.
     */
    public val contextViews: List<ContextViewResult>,

    /**
     * Total count of context views.
     */
    public val totalCount: Int,
)
