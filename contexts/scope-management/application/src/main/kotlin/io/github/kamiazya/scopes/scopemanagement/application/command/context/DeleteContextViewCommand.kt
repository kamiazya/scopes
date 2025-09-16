package io.github.kamiazya.scopes.scopemanagement.application.command.context

/**
 * Command to delete a context view.
 * Follows CQRS naming convention where all commands end with 'Command' suffix.
 *
 * This command validates the key, ensures the context view exists,
 * checks if it's not currently active, and deletes it from the repository.
 */
data class DeleteContextViewCommand(val key: String)
