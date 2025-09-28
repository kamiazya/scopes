package io.github.kamiazya.scopes.contracts.userpreferences.results

/**
 * Base interface for all preference results.
 * This sealed interface allows for type-safe preference handling across different preference types.
 */
public sealed interface PreferenceResult {
    /**
     * Hierarchy preferences result.
     */
    public data class HierarchyPreferences(public val maxDepth: Int? = null, public val maxChildrenPerScope: Int? = null) : PreferenceResult
}
