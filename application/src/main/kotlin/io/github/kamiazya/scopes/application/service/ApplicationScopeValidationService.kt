package io.github.kamiazya.scopes.application.service

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
 * Application service for repository-dependent scope validation logic.
 *
 * This service contains validation methods that require repository access,
 * migrated from ScopeValidationService to maintain proper domain layer isolation.
 *
 * Contains all validation constants and repository-dependent validation logic.
 */
class ApplicationScopeValidationService(
    private val repository: ScopeRepository
) {

    companion object {
        const val MAX_HIERARCHY_DEPTH = 10
        const val MAX_CHILDREN_PER_PARENT = 100
    }

    /**
     * Helper extension function to convert Either results into ValidationResult.
     */
    private fun <E : DomainError, T> Either<E, T>.toValidationResult(): ValidationResult<T> =
        fold({ it.validationFailure() }, { it.validationSuccess() })

    /**
     * Comprehensive validation for scope creation using accumulating ValidationResult.
     * This single method handles all validation concerns and accumulates errors.
     * Use .firstErrorOnly() for fail-fast behavior when needed.
     */
    suspend fun validateScopeCreation(
        title: String,
        description: String?,
        parentId: ScopeId?
    ): ValidationResult<Unit> {
        val validations = listOf(
            ScopeTitle.create(title).toValidationResult().map { },
            ScopeDescription.create(description).toValidationResult().map { },
            validateHierarchyDepth(parentId).toValidationResult(),
            validateChildrenLimit(parentId).toValidationResult(),
            validateTitleUniqueness(title, parentId).toValidationResult()
        )

        return validations.sequence().map { }
    }

    /**
     * Validate hierarchy depth using repository query.
     */
    suspend fun validateHierarchyDepth(
        parentId: ScopeId?
    ): Either<DomainError, Unit> = either {
        if (parentId == null) {
            return@either
        }

        val depth = repository.findHierarchyDepth(parentId)
            .mapLeft { DomainError.InfrastructureError(it) }
            .bind()

        ensure(depth < MAX_HIERARCHY_DEPTH) {
            DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(MAX_HIERARCHY_DEPTH, depth + 1)
        }
    }

    /**
     * Validate children limit using repository query.
     */
    suspend fun validateChildrenLimit(
        parentId: ScopeId?
    ): Either<DomainError, Unit> = either {
        if (parentId == null) {
            return@either
        }

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
     * Validate title uniqueness using repository query.
     * All scopes (including root level) must have unique titles within their context.
     * Normalizes titles by trimming whitespace and converting to lowercase for consistent comparison.
     */
    suspend fun validateTitleUniqueness(
        title: String,
        parentId: ScopeId?
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
}
