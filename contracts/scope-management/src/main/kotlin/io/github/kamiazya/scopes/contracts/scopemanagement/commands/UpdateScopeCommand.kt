package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command for updating an existing scope.
 *
 * This is a minimal contract for scope updates that contains only
 * the essential fields needed by external consumers.
 */
public data class UpdateScopeCommand(
    public val id: String,
    public val title: String? = null,
    public val description: String? = null,
    public val parentId: String? = null,
)
