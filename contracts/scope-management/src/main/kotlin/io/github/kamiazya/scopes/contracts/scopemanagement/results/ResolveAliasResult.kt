package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of resolving an alias or prefix to scope information.
 */
public data class ResolveAliasResult(
    /**
     * The resolved scope ID.
     */
    public val scopeId: String,

    /**
     * The exact alias that was matched.
     */
    public val matchedAlias: String,

    /**
     * Whether this was a prefix match (true) or exact match (false).
     */
    public val wasPrefixMatch: Boolean = false,

    /**
     * Other aliases that also matched the prefix (if it was a prefix search).
     * Empty list for exact matches.
     */
    public val otherMatches: List<String> = emptyList(),
)
