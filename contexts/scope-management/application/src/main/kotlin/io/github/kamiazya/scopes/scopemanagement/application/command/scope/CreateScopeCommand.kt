package io.github.kamiazya.scopes.scopemanagement.application.command.scope

/**
 * Command for creating a new scope.
 * Named with 'Command' suffix to follow CQRS naming conventions.
 * Contains all necessary data for scope creation.
 * Uses primitive types to avoid leaking domain concepts.
 */
data class CreateScopeCommand(
    val title: String,
    val description: String? = null,
    val parentId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val generateAlias: Boolean = true, // Auto-generate canonical alias by default
    val customAlias: String? = null, // Optionally provide a custom canonical alias
)
