package io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect

/**
 * Command for deleting an aspect definition.
 * Follows CQRS naming convention where all commands end with 'Command' suffix.
 * Validates that the aspect is not in use before allowing deletion.
 */
data class DeleteAspectDefinitionCommand(val key: String)
