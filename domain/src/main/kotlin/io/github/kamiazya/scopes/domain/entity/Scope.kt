package io.github.kamiazya.scopes.domain.entity

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.NonEmptyList
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.Aspects
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Core domain entity representing a unified "Scope" that can be a project, epic, or task.
 * This implements the recursive structure where all entities share the same operations.
 * Follows functional DDD principles with immutability and pure functions.
 * Supports aspect-based classification for flexible metadata management.
 */
data class Scope(
    val id: ScopeId,
    val title: ScopeTitle,
    val description: ScopeDescription? = null,
    val parentId: ScopeId? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val aspects: Aspects = Aspects.empty(),
) {
    companion object {
        /**
         * Create a new scope with generated timestamps.
         * This is the safe factory method that validates input.
         */
        fun create(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            aspectsData: Map<AspectKey, NonEmptyList<AspectValue>> = emptyMap()
        ): Either<ScopesError, Scope> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()

            val now = Clock.System.now()
            Scope(
                id = ScopeId.generate(),
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
                createdAt = now,
                updatedAt = now,
                aspects = Aspects.from(aspectsData)
            )
        }

        /**
         * Create a new scope with explicit ID and generated timestamps.
         * This is used internally when you already have a validated ID.
         * Internal visibility ensures it can only be used within the domain module.
         */
        internal fun create(
            id: ScopeId,
            title: ScopeTitle,
            description: ScopeDescription? = null,
            parentId: ScopeId? = null,
            aspectsData: Map<AspectKey, NonEmptyList<AspectValue>> = emptyMap()
        ): Scope {
            val now = Clock.System.now()
            return Scope(
                id = id,
                title = title,
                description = description,
                parentId = parentId,
                createdAt = now,
                updatedAt = now,
                aspects = Aspects.from(aspectsData)
            )
        }
    }

    /**
     * Update the scope title with new timestamp.
     * Pure function that returns a new instance.
     */
    fun updateTitle(newTitle: String): Either<ScopesError, Scope> = either {
        val validatedTitle = ScopeTitle.create(newTitle).bind()
        copy(title = validatedTitle, updatedAt = Clock.System.now())
    }

    /**
     * Update the scope description with new timestamp.
     * Pure function that returns a new instance.
     */
    fun updateDescription(newDescription: String?): Either<ScopesError, Scope> = either {
        val validatedDescription = ScopeDescription.create(newDescription).bind()
        copy(description = validatedDescription, updatedAt = Clock.System.now())
    }

    /**
     * Move scope to a new parent with new timestamp.
     * Pure function that returns a new instance.
     */
    fun moveToParent(newParentId: ScopeId?): Scope =
        copy(parentId = newParentId, updatedAt = Clock.System.now())

    // ===== ASPECT MANAGEMENT METHODS =====

    /**
     * Set an aspect with new timestamp.
     * Pure function that returns a new instance.
     */
    fun setAspect(key: AspectKey, values: NonEmptyList<AspectValue>): Scope =
        copy(
            aspects = aspects.set(key, values),
            updatedAt = Clock.System.now()
        )

    /**
     * Set a single aspect value (convenience method).
     * Pure function that returns a new instance.
     */
    fun setAspect(key: AspectKey, value: AspectValue): Scope =
        copy(
            aspects = aspects.set(key, value),
            updatedAt = Clock.System.now()
        )

    /**
     * Remove an aspect with new timestamp.
     * Pure function that returns a new instance.
     */
    fun removeAspect(key: AspectKey): Scope =
        copy(
            aspects = aspects.remove(key),
            updatedAt = Clock.System.now()
        )

    /**
     * Get all aspects as a map of AspectKey to NonEmptyList<AspectValue>.
     */
    fun getAspects(): Map<AspectKey, NonEmptyList<AspectValue>> = aspects.toMap()

    /**
     * Get a specific aspect value (returns first value if multiple).
     */
    fun getAspectValue(key: AspectKey): AspectValue? = aspects.getFirst(key)

    /**
     * Get all values for a specific aspect.
     */
    fun getAspectValues(key: AspectKey): NonEmptyList<AspectValue>? = aspects.get(key)

    /**
     * Check if this scope has a specific aspect.
     */
    fun hasAspect(key: AspectKey): Boolean = aspects.contains(key)

    /**
     * Set multiple aspects with new timestamp.
     * Pure function that returns a new instance.
     */
    fun setAspects(newAspects: Map<AspectKey, NonEmptyList<AspectValue>>): Scope =
        copy(
            aspects = aspects.merge(Aspects.from(newAspects)),
            updatedAt = Clock.System.now()
        )

    /**
     * Remove multiple aspects with new timestamp.
     * Pure function that returns a new instance.
     */
    fun removeAspects(keys: List<AspectKey>): Scope =
        copy(
            aspects = aspects.remove(keys.toSet()),
            updatedAt = Clock.System.now()
        )

    /**
     * Clear all aspects with new timestamp.
     * Pure function that returns a new instance.
     */
    fun clearAspects(): Scope =
        copy(aspects = Aspects.empty(), updatedAt = Clock.System.now())

    // ===== BUSINESS RULES =====

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

