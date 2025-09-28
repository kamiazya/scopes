package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of removing an alias from a scope.
 */
public data class RemoveAliasResult(
    /**
     * The ID of the scope that the alias was removed from.
     */
    public val scopeId: String,

    /**
     * The alias that was removed.
     */
    public val removedAlias: String,
)
