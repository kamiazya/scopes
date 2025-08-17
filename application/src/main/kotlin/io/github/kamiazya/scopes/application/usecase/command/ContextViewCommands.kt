package io.github.kamiazya.scopes.application.usecase.command

import io.github.kamiazya.scopes.application.usecase.query.Query

/**
 * Commands and queries for context view operations.
 * These represent user intents and contain all necessary data for the operation.
 */

/**
 * Create a new named context view.
 */
data class CreateContextView(
    val name: String,
    val filterExpression: String,
    val description: String? = null
) : Command

/**
 * Update an existing context view.
 */
data class UpdateContextView(
    val id: String,
    val name: String? = null,
    val filterExpression: String? = null,
    val description: String? = null
) : Command

/**
 * Delete a context view.
 */
data class DeleteContextView(
    val id: String
) : Command

/**
 * Switch to a different context view.
 */
data class SwitchContext(
    val name: String
) : Command

/**
 * List context views with optional filtering.
 */
data class ListContextViews(
    val includeInactive: Boolean = true
) : Query

/**
 * Apply current context filter to get filtered scopes.
 */
data class GetFilteredScopes(
    val contextName: String? = null // If null, use currently active context
) : Query
