package io.github.kamiazya.scopes.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.validationSuccess
import io.github.kamiazya.scopes.domain.error.validationFailure
import io.github.kamiazya.scopes.domain.error.sequence
import io.github.kamiazya.scopes.domain.error.map
import io.github.kamiazya.scopes.domain.repository.ScopeRepository

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

    // ===== BASIC FIELD VALIDATION =====

    /**
     * Validates that a title is not empty or blank.
     */
    fun validateTitle(title: String): Either<DomainError.ValidationError, String> = either {
        ensure(title.trim().isNotEmpty()) { DomainError.ValidationError.EmptyTitle }
        title
    }

    /**
     * Validates that a description doesn't exceed maximum length.
     */
    fun validateDescription(description: String?): Either<DomainError.ValidationError, String?> = either {
        if (description != null) {
            ensure(description.length <= ScopeDescription.MAX_LENGTH) {
                DomainError.ValidationError.DescriptionTooLong(ScopeDescription.MAX_LENGTH, description.length)
            }
        }
        description
    }

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
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        if (parentId == null) return@either

        val depth = repository.findHierarchyDepth(parentId)
            .mapLeft {
                // Repository errors should be handled at application layer
                // For now, treat as max depth exceeded to maintain business rule context
                DomainError.BusinessRuleViolation.MaxDepthExceeded(
                    MAX_HIERARCHY_DEPTH,
                    MAX_HIERARCHY_DEPTH + 1
                )
            }
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
            .mapLeft {
                // Repository errors should be handled at application layer
                // For now, treat as max children exceeded to maintain business rule context
                DomainError.BusinessRuleViolation.MaxChildrenExceeded(
                    MAX_CHILDREN_PER_PARENT,
                    MAX_CHILDREN_PER_PARENT + 1
                )
            }
            .bind()

        ensure(childrenCount < MAX_CHILDREN_PER_PARENT) {
            DomainError.BusinessRuleViolation.MaxChildrenExceeded(
                MAX_CHILDREN_PER_PARENT, childrenCount + 1
            )
        }
    }

    /**
     * Efficient version: Validate title uniqueness using repository query.
     * Note: Root level scopes (parentId = null) allow duplicate titles.
     */
    suspend fun validateTitleUniquenessEfficient(
        title: String,
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        // Allow duplicate titles at root level (parentId = null)
        if (parentId != null) {
            val duplicateExists = repository.existsByParentIdAndTitle(parentId, title)
                .mapLeft {
                    // Repository errors should be handled at application layer
                    // For now, treat as duplicate title to maintain business rule context
                    DomainError.BusinessRuleViolation.DuplicateTitle(title, parentId)
                }
                .bind()

            ensure(!duplicateExists) {
                DomainError.BusinessRuleViolation.DuplicateTitle(title, parentId)
            }
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
            validateTitle(title).toValidationResult(),
            validateDescription(description).toValidationResult(),
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
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        ensure(parentDepth < MAX_HIERARCHY_DEPTH) {
            DomainError.BusinessRuleViolation.MaxDepthExceeded(MAX_HIERARCHY_DEPTH, parentDepth + 1)
        }
    }

    /**
     * Validate children limit using pre-computed children count.
     * Pure function that doesn't depend on repository access.
     */
    fun validateChildrenLimitWithContext(
        currentChildrenCount: Int
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        ensure(currentChildrenCount < MAX_CHILDREN_PER_PARENT) {
            DomainError.BusinessRuleViolation.MaxChildrenExceeded(
                MAX_CHILDREN_PER_PARENT, currentChildrenCount + 1
            )
        }
    }

    /**
     * Validate title uniqueness using pre-computed title existence check.
     * Pure function that doesn't depend on repository access.
     * Note: Root level scopes (parentId = null) allow duplicate titles.
     */
    fun validateTitleUniquenessWithContext(
        titleExists: Boolean,
        title: String,
        parentId: ScopeId?
    ): Either<DomainError.BusinessRuleViolation, Unit> = either {
        // Allow duplicate titles at root level (parentId = null)
        if (parentId != null) {
            ensure(!titleExists) {
                DomainError.BusinessRuleViolation.DuplicateTitle(title, parentId)
            }
        }
    }

}
