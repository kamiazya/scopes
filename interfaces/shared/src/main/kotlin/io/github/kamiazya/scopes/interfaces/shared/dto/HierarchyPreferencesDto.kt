package io.github.kamiazya.scopes.interfaces.shared.dto

/**
 * Data Transfer Object for hierarchy preferences.
 *
 * Contains only the user preferences related to scope hierarchy limits.
 * Null indicates preference not set; provider determines the default (currently unlimited).
 */
data class HierarchyPreferencesDto(
    val maxDepth: Int? = null, // null indicates preference not set; provider determines the default (currently unlimited)
    val maxChildrenPerScope: Int? = null, // null indicates preference not set; provider determines the default (currently unlimited)
)
