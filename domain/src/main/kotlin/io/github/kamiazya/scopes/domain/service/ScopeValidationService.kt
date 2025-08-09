package io.github.kamiazya.scopes.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.validationFailure
import io.github.kamiazya.scopes.domain.error.validationSuccess
import io.github.kamiazya.scopes.domain.error.sequence
import io.github.kamiazya.scopes.domain.error.map
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle

/**
 * Domain service for scope validation logic.
 * Contains business rules that don't naturally belong to any single entity.
 * All functions are pure and stateless following functional DDD principles.
 */
object ScopeValidationService {

    // Constants moved to value objects for better encapsulation
    // Use ScopeTitle.MAX_LENGTH, ScopeDescription.MAX_LENGTH instead
    const val MAX_HIERARCHY_DEPTH = 10
    const val MAX_CHILDREN_PER_PARENT = 100


    /**
     * Helper extension function to convert Either results into ValidationResult.
     * Eliminates repetitive fold calls throughout validation functions.
     */
    private fun <E : DomainError, T> Either<E, T>.toValidationResult(): ValidationResult<T> =
        fold({ it.validationFailure() }, { it.validationSuccess() })

    /**
     * Efficient version: Validate hierarchy depth using repository query.
     */
    suspend fun validateHierarchyDepthEfficient(
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError, Unit> = either {
        if (parentId == null) return@either

        val depth = repository.findHierarchyDepth(parentId)
            .mapLeft { DomainError.InfrastructureError(it) }
            .bind()

        ensure(depth < MAX_HIERARCHY_DEPTH) {
            DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(MAX_HIERARCHY_DEPTH, depth + 1)
        }
    }

    /**
     * Efficient version: Validate children limit using repository query.
     */
    suspend fun validateChildrenLimitEfficient(
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError, Unit> = either {
        if (parentId == null) return@either

        val childrenCount = repository.countByParentId(parentId)
            .mapLeft { DomainError.InfrastructureError(it) }
            .bind()

        ensure(childrenCount < MAX_CHILDREN_PER_PARENT) {
            DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(
                MAX_CHILDREN_PER_PARENT, childrenCount + 1
            )
        }
    }

    /**
     * Efficient version: Validate title uniqueness using repository query.
     * All scopes (including root level) must have unique titles within their context.
     */
    suspend fun validateTitleUniquenessEfficient(
        title: String,
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError, Unit> = either {
        val duplicateExists = repository.existsByParentIdAndTitle(parentId, title)
            .mapLeft { DomainError.InfrastructureError(it) }
            .bind()

        ensure(!duplicateExists) {
            DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle(title, parentId)
        }
    }

    // ===== CONSOLIDATED VALIDATION METHODS =====

    /**
     * Comprehensive validation for scope creation using accumulating ValidationResult.
     * This single method handles all validation concerns and accumulates errors.
     * Use .firstErrorOnly() for fail-fast behavior when needed.
     */
    suspend fun validateScopeCreation(
        title: String,
        description: String?,
        parentId: ScopeId?,
        repository: ScopeRepository
    ): ValidationResult<Unit> {
        val validations = listOf(
            ScopeTitle.create(title).toValidationResult().map { },
            ScopeDescription.create(description).toValidationResult().map { },
            validateHierarchyDepthEfficient(parentId, repository).toValidationResult(),
            validateChildrenLimitEfficient(parentId, repository).toValidationResult(),
            validateTitleUniquenessEfficient(title, parentId, repository).toValidationResult()
        )

        return validations.sequence().map { }
    }

    // ===== PURE HIERARCHY CONTEXT-BASED VALIDATION FUNCTIONS =====

    /**
     * Validate hierarchy depth using pre-computed parent depth.
     * Pure function that doesn't depend on repository access.
     */
    fun validateHierarchyDepthWithContext(
        parentDepth: Int
    ): Either<DomainError.ScopeBusinessRuleViolation, Unit> = either {
        ensure(parentDepth < MAX_HIERARCHY_DEPTH) {
            DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(MAX_HIERARCHY_DEPTH, parentDepth + 1)
        }
    }

    /**
     * Validate children limit using pre-computed children count.
     * Pure function that doesn't depend on repository access.
     */
    fun validateChildrenLimitWithContext(
        currentChildrenCount: Int
    ): Either<DomainError.ScopeBusinessRuleViolation, Unit> = either {
        ensure(currentChildrenCount < MAX_CHILDREN_PER_PARENT) {
            DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(
                MAX_CHILDREN_PER_PARENT, currentChildrenCount + 1
            )
        }
    }

    /**
     * Validate title uniqueness using pre-computed title existence check.
     * Pure function that doesn't depend on repository access.
     * All scopes (including root level) must have unique titles within their context.
     */
    fun validateTitleUniquenessWithContext(
        existsInParentContext: Boolean,
        title: String,
        parentId: ScopeId?
    ): Either<DomainError.ScopeBusinessRuleViolation, Unit> = either {
        ensure(!existsInParentContext) {
            DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle(title, parentId)
        }
    }

}
