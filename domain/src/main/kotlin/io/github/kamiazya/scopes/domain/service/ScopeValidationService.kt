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
 * Context data for pure domain validation without repository dependencies.
 *
 * @param title The scope title to validate
 * @param description The scope description to validate (nullable)
 * @param parentDepth The pre-computed depth of the parent (0 for root level)
 * @param currentChildrenCount The pre-computed count of existing children under the parent
 * @param existsInParentContext Whether a scope with the same title already exists in the parent context
 * @param parentId The parent scope ID for error context (nullable for root level)
 */
data class ValidationContext(
    val title: String,
    val description: String?,
    val parentDepth: Int,
    val currentChildrenCount: Int,
    val existsInParentContext: Boolean,
    val parentId: ScopeId?
)

/**
 * Domain service for scope validation logic.
 * Contains business rules that don't naturally belong to any single entity.
 *
 * This service focuses on pure domain validation using the WithContext methods.
 * Repository-dependent methods are deprecated and will be moved to the application
 * layer in a follow-up PR to maintain proper domain layer isolation.
 *
 * All WithContext functions are pure and stateless following functional DDD principles.
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
     * @deprecated This method will be moved to the application layer in a follow-up PR.
     * Use validateHierarchyDepthWithContext() instead for pure domain validation.
     *
     * Efficient version: Validate hierarchy depth using repository query.
     */
    @Deprecated(
        message = "Repository-dependent methods should be moved to application layer. " +
            "Use validateHierarchyDepthWithContext() instead.",
        replaceWith = ReplaceWith("validateHierarchyDepthWithContext(parentDepth)")
    )
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
     * @deprecated This method will be moved to the application layer in a follow-up PR.
     * Use validateChildrenLimitWithContext() instead for pure domain validation.
     *
     * Efficient version: Validate children limit using repository query.
     */
    @Deprecated(
        message = "Repository-dependent methods should be moved to application layer. " +
            "Use validateChildrenLimitWithContext() instead.",
        replaceWith = ReplaceWith("validateChildrenLimitWithContext(currentChildrenCount)")
    )
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
     * @deprecated This method will be moved to the application layer in a follow-up PR.
     * Use validateTitleUniquenessWithContext() instead for pure domain validation.
     *
     * Efficient version: Validate title uniqueness using repository query.
     * All scopes (including root level) must have unique titles within their context.
     * Normalizes titles by trimming whitespace and converting to lowercase for consistent comparison.
     */
    @Deprecated(
        message = "Repository-dependent methods should be moved to application layer. " +
            "Use validateTitleUniquenessWithContext() instead.",
        replaceWith = ReplaceWith("validateTitleUniquenessWithContext(existsInParentContext, title, parentId)")
    )
    suspend fun validateTitleUniquenessEfficient(
        title: String,
        parentId: ScopeId?,
        repository: ScopeRepository
    ): Either<DomainError, Unit> = either {
        // Normalize title for consistent duplicate detection
        val normalizedTitle = title.trim().lowercase()
        val duplicateExists = repository.existsByParentIdAndTitle(parentId, normalizedTitle)
            .mapLeft { DomainError.InfrastructureError(it) }
            .bind()

        ensure(!duplicateExists) {
            DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle(title, parentId)
        }
    }

    // ===== DEPRECATED REPOSITORY-DEPENDENT METHODS =====
    // These methods will be moved to the application layer in a follow-up PR to maintain
    // proper domain layer isolation. A new ApplicationScopeValidationService will be created
    // to house these methods, and all usages will be updated accordingly.

    /**
     * @deprecated This method will be moved to the application layer in a follow-up PR.
     * Use the WithContext validation methods for pure domain validation.
     *
     * Comprehensive validation for scope creation using accumulating ValidationResult.
     * This single method handles all validation concerns and accumulates errors.
     * Use .firstErrorOnly() for fail-fast behavior when needed.
     */
    @Deprecated(
        message = "Repository-dependent methods should be moved to application layer. " +
            "Use WithContext validation methods instead.",
        level = DeprecationLevel.WARNING
    )
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

    // ===== PURE CONTEXT-BASED VALIDATION METHODS =====

    fun validateScopeCreationWithContext(
        context: ValidationContext
    ): ValidationResult<Unit> {
        val validations = listOf(
            ScopeTitle.create(context.title).toValidationResult().map { },
            ScopeDescription.create(context.description).toValidationResult().map { },
            validateHierarchyDepthWithContext(context.parentDepth).toValidationResult(),
            validateChildrenLimitWithContext(context.currentChildrenCount).toValidationResult(),
            validateTitleUniquenessWithContext(
                context.existsInParentContext,
                context.title,
                context.parentId
            ).toValidationResult()
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
