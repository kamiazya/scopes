package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command to create a new aspect definition.
 */
public data class CreateAspectDefinitionCommand(val key: String, val description: String, val type: String)

/**
 * Command to update an aspect definition.
 */
public data class UpdateAspectDefinitionCommand(val key: String, val description: String? = null)

/**
 * Command to delete an aspect definition.
 */
public data class DeleteAspectDefinitionCommand(val key: String)
