package io.github.kamiazya.scopes.scopemanagement.application.command.aspect

/**
 * Command for updating an existing aspect definition.
 * Follows CQRS naming convention where all commands end with 'Command' suffix.
 * Currently only supports updating the description, as changing the type
 * could break existing data.
 */
data class UpdateAspectDefinitionCommand(val key: String, val description: String? = null)
