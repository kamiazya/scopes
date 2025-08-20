package io.github.kamiazya.scopes.application.usecase.command

import io.github.kamiazya.scopes.application.dto.DTO

/**
 * Command for creating a new scope.
 * Contains all necessary data for scope creation.
 * Uses primitive types to avoid leaking domain concepts.
 */
data class CreateScope(
    val title: String,
    val description: String? = null,
    val parentId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val generateAlias: Boolean = true, // Auto-generate canonical alias by default
    val customAlias: String? = null, // Optionally provide a custom canonical alias
) : Command,
    DTO
