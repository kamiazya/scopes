package io.github.kamiazya.scopes.scopemanagement.application.command.aspect

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType

/**
 * Command for defining a new aspect.
 * Follows CQRS naming convention where all commands end with 'Command' suffix.
 * Creates and persists an AspectDefinition with the specified type.
 */
data class DefineAspectCommand(val key: String, val description: String, val type: AspectType)
