package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command for creating a new scope.
 *
 * This is a minimal contract for scope creation that contains only
 * the essential fields needed by external consumers.
 */
public data class CreateScopeCommand(
    public val title: String,
    public val description: String? = null,
    public val parentId: String? = null,
    public val generateAlias: Boolean = true,
    public val customAlias: String? = null,
)
