package com.kamiazya.scopes.domain.entity

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Core domain entity representing a unified "Scope" that can be a project, epic, or task.
 * This implements the recursive structure where all entities share the same operations.
 * Follows functional DDD principles with immutability and pure functions.
 * Priority and Status will be implemented as Aspects in the future.
 */
@Serializable
data class Scope(
    val id: ScopeId,
    val title: String,
    val description: String? = null,
    val parentId: ScopeId? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
) {
    companion object {
        /**
         * Create a new scope with generated timestamps.
         */
        fun create(
            id: ScopeId,
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            metadata: Map<String, String> = emptyMap()
        ): Scope {
            val now = Clock.System.now()
            return Scope(
                id = id,
                title = title,
                description = description,
                parentId = parentId,
                createdAt = now,
                updatedAt = now,
                metadata = metadata
            )
        }
    }

    /**
     * Update the scope title with new timestamp.
     * Pure function that returns a new instance.
     */
    fun updateTitle(newTitle: String): Scope =
        copy(title = newTitle, updatedAt = Clock.System.now())

    /**
     * Update the scope description with new timestamp.
     * Pure function that returns a new instance.
     */
    fun updateDescription(newDescription: String?): Scope =
        copy(description = newDescription, updatedAt = Clock.System.now())

    /**
     * Move scope to a new parent with new timestamp.
     * Pure function that returns a new instance.
     */
    fun moveToParent(newParentId: ScopeId?): Scope =
        copy(parentId = newParentId, updatedAt = Clock.System.now())

    /**
     * Add or update metadata with new timestamp.
     * Pure function that returns a new instance.
     */
    fun updateMetadata(key: String, value: String): Scope =
        copy(metadata = metadata + (key to value), updatedAt = Clock.System.now())

    /**
     * Remove metadata with new timestamp.
     * Pure function that returns a new instance.
     */
    fun removeMetadata(key: String): Scope =
        copy(metadata = metadata - key, updatedAt = Clock.System.now())

    /**
     * Business rule: Check if this scope can be a parent of another scope.
     * Prevents circular references and self-parenting.
     */
    fun canBeParentOf(childScope: Scope): Boolean =
        id != childScope.id && childScope.parentId != id

    /**
     * Check if this scope is a child of the specified parent.
     */
    fun isChildOf(potentialParent: Scope): Boolean =
        parentId == potentialParent.id

    /**
     * Check if this scope is a root scope (no parent).
     */
    fun isRoot(): Boolean = parentId == null
}

