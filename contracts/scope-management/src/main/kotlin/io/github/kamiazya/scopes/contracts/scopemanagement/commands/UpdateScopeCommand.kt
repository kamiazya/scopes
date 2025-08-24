package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command for updating an existing scope.
 *
 * This is a minimal contract for scope updates that contains only
 * the essential fields needed by external consumers.
 */
data class UpdateScopeCommand(val id: String, val title: String? = null, val description: String? = null, val parentId: String? = null)
