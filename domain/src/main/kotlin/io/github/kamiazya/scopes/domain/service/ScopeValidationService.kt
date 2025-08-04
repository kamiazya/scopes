package io.github.kamiazya.scopes.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository

/**
 * Domain service for scope validation logic.
 * Contains business rules that don't naturally belong to any single entity.
 * All functions are pure and stateless following functional DDD principles.
 */
object ScopeValidationService {

    const val MAX_TITLE_LENGTH = 200
    const val MIN_TITLE_LENGTH = 1
    const val MAX_DESCRIPTION_LENGTH = 1000
    const val MAX_HIERARCHY_DEPTH = 10
    const val MAX_CHILDREN_PER_PARENT = 100

    /**
     * Validate scope title according to business rules.
     */
    fun validateTitle(title: String): Either<DomainError.ValidationError, String> = either {
        ensure(title.isNotBlank()) { DomainError.ValidationError.EmptyTitle }
        ensure(title.length >= MIN_TITLE_LENGTH) { DomainError.ValidationError.TitleTooShort }
        ensure(title.length <= MAX_TITLE_LENGTH) {
            DomainError.ValidationError.TitleTooLong(MAX_TITLE_LENGTH, title.length)
        }
        title.trim()
    }

    /**
     * Validate scope description according to business rules.
     */
    fun validateDescription(description: String?): Either<DomainError.ValidationError, String?> = either {
        when (description) {
            null -> null
            else -> {
                ensure(description.length <= MAX_DESCRIPTION_LENGTH) {
                    DomainError.ValidationError.DescriptionTooLong(MAX_DESCRIPTION_LENGTH, description.length)
                }
                description.trim().ifBlank { null }
            }
        }
    }

    /**
     * Validate that a scope can be moved to a new parent.
     * Prevents circular references in the hierarchy.
     */
    fun validateParentRelationship(
        scope: Scope,
        newParentId: ScopeId?,
        allScopes: List<Scope>
    ): Either<DomainError.ScopeError, Unit> = either {
        if (newParentId == null) return@either

        ensure(newParentId != scope.id) { DomainError.ScopeError.SelfParenting }

        // Check for circular reference
        val ancestors = generateSequence(newParentId) { currentId ->
            allScopes.find { it.id == currentId }?.parentId
        }

        ensure(scope.id !in ancestors) {
            DomainError.ScopeError.CircularReference(scope.id, newParentId)
        }
    }

    /**
     * Validate hierarchy depth doesn't exceed maximum.
     */
    fun validateHierarchyDepth(
        parentId: ScopeId?,
        allScopes: List<Scope>
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        if (parentId == null) return@either

        val depth = calculateDepth(parentId, allScopes)
        ensure(depth < MAX_HIERARCHY_DEPTH) {
            DomainError.BusinessRuleViolation.MaxDepthExceeded(MAX_HIERARCHY_DEPTH, depth + 1)
        }
    }

    /**
     * Validate that parent doesn't exceed maximum children limit.
     */
    fun validateChildrenLimit(
        parentId: ScopeId?,
        allScopes: List<Scope>
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        if (parentId == null) return@either

        val childrenCount = allScopes.count { it.parentId == parentId }
        ensure(childrenCount < MAX_CHILDREN_PER_PARENT) {
            DomainError.BusinessRuleViolation.MaxChildrenExceeded(
                MAX_CHILDREN_PER_PARENT, childrenCount + 1
            )
        }
    }

    /**
     * Validate title uniqueness within the same parent scope.
     */
    fun validateTitleUniqueness(
        title: String,
        parentId: ScopeId?,
        excludeScopeId: ScopeId?,
        allScopes: List<Scope>
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        val duplicateExists = allScopes.any { scope ->
            scope.id != excludeScopeId &&
            scope.parentId == parentId &&
            scope.title.equals(title, ignoreCase = true)
        }

        ensure(!duplicateExists) {
            DomainError.BusinessRuleViolation.DuplicateTitle(title, parentId)
        }
    }

    /**
     * Efficient version: Validate hierarchy depth using repository query.
     */
    suspend fun validateHierarchyDepthEfficient(
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        if (parentId == null) return@either

        val depth = repository.findHierarchyDepth(parentId)
            .mapLeft { DomainError.BusinessRuleViolation.DatabaseError(it.toString()) }
            .bind()

        ensure(depth < MAX_HIERARCHY_DEPTH) {
            DomainError.BusinessRuleViolation.MaxDepthExceeded(MAX_HIERARCHY_DEPTH, depth + 1)
        }
    }

    /**
     * Efficient version: Validate children limit using repository query.
     */
    suspend fun validateChildrenLimitEfficient(
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        if (parentId == null) return@either

        val childrenCount = repository.countByParentId(parentId)
            .mapLeft { DomainError.BusinessRuleViolation.DatabaseError(it.toString()) }
            .bind()

        ensure(childrenCount < MAX_CHILDREN_PER_PARENT) {
            DomainError.BusinessRuleViolation.MaxChildrenExceeded(
                MAX_CHILDREN_PER_PARENT, childrenCount + 1
            )
        }
    }

    /**
     * Efficient version: Validate title uniqueness using repository query.
     */
    suspend fun validateTitleUniquenessEfficient(
        title: String,
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        val duplicateExists = repository.existsByParentIdAndTitle(parentId, title)
            .mapLeft { DomainError.BusinessRuleViolation.DatabaseError(it.toString()) }
            .bind()

        ensure(!duplicateExists) {
            DomainError.BusinessRuleViolation.DuplicateTitle(title, parentId)
        }
    }

    /**
     * Calculate the depth of a scope in the hierarchy.
     */
    private fun calculateDepth(scopeId: ScopeId, allScopes: List<Scope>): Int {
        tailrec fun calculateDepthRecursive(currentId: ScopeId?, depth: Int): Int =
            when (currentId) {
                null -> depth
                else -> {
                    val parent = allScopes.find { it.id == currentId }?.parentId
                    calculateDepthRecursive(parent, depth + 1)
                }
            }

        return calculateDepthRecursive(scopeId, 0)
    }
}
