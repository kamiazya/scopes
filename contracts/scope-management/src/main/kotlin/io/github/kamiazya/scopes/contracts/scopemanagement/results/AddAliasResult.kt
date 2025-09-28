package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of adding an alias to a scope.
 */
public data class AddAliasResult(
    /**
     * The ID of the scope that the alias was added to.
     */
    public val scopeId: String,

    /**
     * The alias that was added.
     */
    public val alias: String,

    /**
     * The type of the alias (e.g., "custom", "generated").
     */
    public val aliasType: String = "custom",
)
