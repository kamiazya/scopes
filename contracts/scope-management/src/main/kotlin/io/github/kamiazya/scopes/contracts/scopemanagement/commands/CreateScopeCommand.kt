package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command for creating a new scope.
 *
 * This is a minimal contract for scope creation that contains only
 * the essential fields needed by external consumers.
 */
data class CreateScopeCommand(
    val title: String,
    val description: String? = null,
    val parentId: String? = null,
    val generateAlias: Boolean = true,
    val customAlias: String? = null,
)
