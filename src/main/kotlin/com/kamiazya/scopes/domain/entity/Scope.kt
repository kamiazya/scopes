package com.kamiazya.scopes.domain.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Core domain entity representing a unified "Scope" that can be a project, epic, or task.
 * This implements the recursive structure where all entities share the same operations.
 * Priority and Status will be implemented as Aspects in the future.
 */
@Serializable
data class Scope(
    val id: ScopeId,
    val title: String,
    val description: String? = null,
    val parentId: ScopeId? = null,
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now(),
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(title.isNotBlank()) { "Scope title cannot be blank" }
    }
}
