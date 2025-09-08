package io.github.kamiazya.scopes.contracts.userpreferences.results

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
public data class HierarchyPreferencesResult(public val maxDepth: Int? = null, public val maxChildrenPerScope: Int? = null)
