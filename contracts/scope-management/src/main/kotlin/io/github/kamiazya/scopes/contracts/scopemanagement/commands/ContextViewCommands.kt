package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command types for context view write operations.
 */
public data class CreateContextViewCommand(val key: String, val name: String, val filter: String, val description: String?)

public data class UpdateContextViewCommand(val key: String, val name: String?, val filter: String?, val description: String?)

public data class DeleteContextViewCommand(val key: String)

public data class SetActiveContextCommand(val key: String)
