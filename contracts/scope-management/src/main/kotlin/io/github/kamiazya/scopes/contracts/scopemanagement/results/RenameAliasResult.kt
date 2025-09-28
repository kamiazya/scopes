package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of renaming/changing the canonical alias of a scope.
 */
public data class RenameAliasResult(
    /**
     * The ID of the scope whose canonical alias was changed.
     */
    public val scopeId: String,

    /**
     * The old canonical alias.
     */
    public val oldCanonicalAlias: String,

    /**
     * The new canonical alias.
     */
    public val newCanonicalAlias: String,

    /**
     * Whether the old alias was retained as a custom alias.
     */
    public val oldAliasRetained: Boolean = true,
)
