package io.github.kamiazya.scopes.application.usecase.command

/**
 * Command to delete a context view.
 */
data class DeleteContext(
    val contextId: String
) : Command