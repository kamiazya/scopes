package io.github.kamiazya.scopes.scopemanagement.domain.entity

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeStatus
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.datetime.Instant

/**
 * Core domain entity representing a unified "Scope" that can be a project, epic, or task.
 * This implements the recursive structure where all entities share the same operations.
 * Follows functional DDD principles with immutability and pure functions.
 *
 * Note: Aspects are temporarily included here but should be managed in aspect-management context.
 */
data class Scope(
    val id: ScopeId,
    val title: ScopeTitle,
    val description: ScopeDescription? = null,
    val parentId: ScopeId? = null,
    val status: ScopeStatus = ScopeStatus.default(),
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
            aspects: Aspects = Aspects.empty(),
            now: Instant,
        ): Either<ScopesError, Scope> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            Scope(
                id = ScopeId.generate(),
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
                createdAt = now,
                status = ScopeStatus.default(),
                updatedAt = now,
                aspects = aspects,
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
            aspects: Aspects = Aspects.empty(),
            now: Instant,
        ): Scope = Scope(
            id = id,
            title = title,
            description = description,
            parentId = parentId,
            createdAt = now,
            status = ScopeStatus.default(),
            updatedAt = now,
            aspects = aspects,
        )
    }

    /**
     * Update the scope title with new timestamp.
     * Pure function that returns a new instance.
     * Only allowed in Draft or Active status.
     */
    fun updateTitle(newTitle: String, now: Instant): Either<ScopesError, Scope> = either {
        ensure(status.canBeEdited()) {
            ScopesError.InvalidOperation(
                operation = "updateTitle",
                entityType = "Scope",
                entityId = id.value,
                reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE,
                occurredAt = now,
            )
        }
        val validatedTitle = ScopeTitle.create(newTitle).bind()
        copy(title = validatedTitle, updatedAt = now)
    }

    /**
     * Update the scope description with new timestamp.
     * Pure function that returns a new instance.
     * Only allowed in Draft or Active status.
     */
    fun updateDescription(newDescription: String?, now: Instant): Either<ScopesError, Scope> = either {
        ensure(status.canBeEdited()) {
            ScopesError.InvalidOperation(
                operation = "updateDescription",
                entityType = "Scope",
                entityId = id.value,
                reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE,
                occurredAt = now,
            )
        }
        val validatedDescription = ScopeDescription.create(newDescription).bind()
        copy(description = validatedDescription, updatedAt = now)
    }

    /**
     * Move scope to a new parent with new timestamp.
     * Pure function that returns a new instance.
     * Only allowed in Draft or Active status.
     */
    fun moveToParent(newParentId: ScopeId?, now: Instant): Either<ScopesError, Scope> = either {
        ensure(status.canBeEdited()) {
            ScopesError.InvalidOperation(
                operation = "moveScope",
                entityType = "Scope",
                entityId = id.value,
                reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE,
                occurredAt = now,
            )
        }
        copy(parentId = newParentId, updatedAt = now)
    }

    // ===== BUSINESS RULES =====

    /**
     * Business rule: Check if this scope can be a parent of another scope.
     * Prevents circular references and self-parenting.
     * Only active scopes can have children.
     */
    fun canBeParentOf(childScope: Scope): Boolean = status.canAddChildren() && id != childScope.id && childScope.parentId != id

    /**
     * Check if this scope is a child of the specified parent.
     */
    fun isChildOf(potentialParent: Scope): Boolean = parentId == potentialParent.id

    /**
     * Check if this scope is a root scope (no parent).
     */
    fun isRoot(): Boolean = parentId == null

    /**
     * Update aspects for this scope.
     * Pure function that returns a new instance with updated timestamp.
     */
    fun updateAspects(newAspects: Aspects, now: Instant): Scope = copy(aspects = newAspects, updatedAt = now)

    /**
     * Add or update a single aspect.
     * Pure function that returns a new instance.
     */
    fun setAspect(key: AspectKey, values: NonEmptyList<AspectValue>, now: Instant): Scope = copy(aspects = aspects.set(key, values), updatedAt = now)

    /**
     * Add or update a single aspect with a single value.
     * Convenience method.
     */
    fun setAspect(key: AspectKey, value: AspectValue, now: Instant): Scope = copy(aspects = aspects.set(key, value), updatedAt = now)

    /**
     * Add a value to an existing aspect key.
     * If the key doesn't exist, creates a new aspect with the single value.
     */
    fun addAspectValue(key: AspectKey, value: AspectValue, now: Instant): Scope = copy(aspects = aspects.add(key, value), updatedAt = now)

    /**
     * Remove an aspect by key.
     * Pure function that returns a new instance.
     */
    fun removeAspect(key: AspectKey, now: Instant): Scope = copy(aspects = aspects.remove(key), updatedAt = now)

    /**
     * Remove a specific value from an aspect key.
     * If this was the last value, the key is removed entirely.
     */
    fun removeAspectValue(key: AspectKey, value: AspectValue, now: Instant): Scope = copy(aspects = aspects.remove(key, value), updatedAt = now)

    /**
     * Clear all aspects.
     * Pure function that returns a new instance.
     */
    fun clearAspects(now: Instant): Scope = copy(aspects = Aspects.empty(), updatedAt = now)

    /**
     * Transition to a new status.
     * Enforces valid state transitions at the domain level.
     */
    fun transitionTo(newStatus: ScopeStatus, now: Instant): Either<ScopesError, Scope> = either {
        status.transitionTo(newStatus).mapLeft {
            ScopesError.InvalidOperation(
                operation = "updateStatus",
                entityType = "Scope",
                entityId = id.value,
                reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE,
                occurredAt = now,
            )
        }.bind()
        copy(status = newStatus, updatedAt = now)
    }

    /**
     * Activate the scope (transition from Draft to Active).
     */
    fun activate(now: Instant): Either<ScopesError, Scope> = transitionTo(ScopeStatus.Active, now)

    /**
     * Complete the scope (transition from Active to Completed).
     */
    fun complete(now: Instant): Either<ScopesError, Scope> = transitionTo(ScopeStatus.Completed, now)

    /**
     * Archive the scope.
     */
    fun archive(now: Instant): Either<ScopesError, Scope> = transitionTo(ScopeStatus.Archived, now)

    /**
     * Reactivate an archived scope.
     */
    fun reactivate(now: Instant): Either<ScopesError, Scope> = transitionTo(ScopeStatus.Active, now)
}
