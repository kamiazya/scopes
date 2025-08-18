package io.github.kamiazya.scopes.application.usecase.command

/**
 * Command to reset an aspect definition to its default value.
 * This removes any user customization for the given aspect key.
 */
data class ResetAspectDefinition(
    val key: String
) : Command

