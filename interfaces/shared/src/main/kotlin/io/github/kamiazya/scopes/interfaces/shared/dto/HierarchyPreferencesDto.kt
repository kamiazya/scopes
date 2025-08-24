package io.github.kamiazya.scopes.interfaces.shared.dto

/**
 * Data Transfer Object for hierarchy preferences.
 *
 * Contains only the user preferences related to scope hierarchy limits.
 * Null values represent no preference set (use system defaults).
 */
data class HierarchyPreferencesDto(
    val maxDepth: Int? = null, // null means unlimited
    val maxChildrenPerScope: Int? = null, // null means unlimited
)
