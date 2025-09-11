package io.github.kamiazya.scopes.scopemanagement.application.command.context

/**
 * Command to update an existing context view.
 * Follows CQRS naming convention where all commands end with 'Command' suffix.
 *
 * @property key The key of the context view to update
 * @property name Optional new name for the context view
 * @property filter Optional new filter expression
 * @property description Optional new description (null means no change, empty string clears it)
 */
data class UpdateContextViewCommand(val key: String, val name: String? = null, val filter: String? = null, val description: String? = null)
