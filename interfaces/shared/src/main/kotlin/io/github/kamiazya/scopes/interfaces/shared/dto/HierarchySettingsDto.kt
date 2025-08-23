package io.github.kamiazya.scopes.interfaces.shared.dto

/**
 * Data Transfer Object for hierarchy settings.
 *
 * Contains only the settings related to scope hierarchy limits.
 * Null values represent unlimited/infinite limits.
 */
data class HierarchySettingsDto(
    val maxDepth: Int? = null, // null means unlimited
    val maxChildrenPerScope: Int? = null, // null means unlimited
)
