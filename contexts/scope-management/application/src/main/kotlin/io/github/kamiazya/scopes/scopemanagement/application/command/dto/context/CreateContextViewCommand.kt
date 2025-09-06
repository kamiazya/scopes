package io.github.kamiazya.scopes.scopemanagement.application.command.dto.context

/**
 * Command to create a new context view.
 * Follows CQRS naming convention where all commands end with 'Command' suffix.
 *
 * @property key Unique identifier for the context view
 * @property name Human-readable name for the context view
 * @property filter Aspect filter expression (e.g., "priority=high AND status=active")
 * @property description Optional description of the context view's purpose
 */
data class CreateContextViewCommand(val key: String, val name: String, val filter: String, val description: String? = null)
