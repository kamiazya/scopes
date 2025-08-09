package io.github.kamiazya.scopes.application.usecase.command

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Command for creating a new scope.
 * Contains all necessary data for scope creation.
 */
data class CreateScope(
    val title: String,
    val description: String? = null,
    val parentId: ScopeId? = null,
    val metadata: Map<String, String> = emptyMap(),
) : Command
