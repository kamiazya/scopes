package io.github.kamiazya.scopes.contracts.userpreferences.results

/**
 * Result containing user preference data.
 *
 * This is a sealed interface to support different preference types.
 * Currently only hierarchy preferences are implemented.
 */
sealed interface PreferenceResult {
    /**
     * Hierarchy preferences result.
     *
     * Contains settings for scope hierarchy limits.
     * Null values indicate the preference is not set; implementations
     * should provide sensible defaults (currently unlimited).
     *
     * @property maxDepth Maximum allowed depth in the scope hierarchy
     * @property maxChildrenPerScope Maximum number of children per scope
     */
    data class HierarchyPreferences(val maxDepth: Int? = null, val maxChildrenPerScope: Int? = null) : PreferenceResult
}
