package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.validationFailure
import io.github.kamiazya.scopes.domain.error.validationSuccess
import io.github.kamiazya.scopes.domain.error.sequence
import io.github.kamiazya.scopes.domain.error.map
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository

import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.domain.util.TitleNormalizer

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
        const val MAX_TITLE_LENGTH = 100
    }

    /**
     * Helper extension function to convert Either results into ValidationResult.
     */
    private fun <E : DomainError, T> Either<E, T>.toValidationResult(): ValidationResult<T> =
        fold({ it.validationFailure() }, { it.validationSuccess() })

    // ===== NEW SERVICE-SPECIFIC ERROR METHODS =====

    /**
     * Validates title format with service-specific error context.
     * Returns specific ScopeValidationServiceError.TitleValidationError types.
     * Delegates to ScopeTitle.create() and maps domain errors to service errors.
     */
    fun validateTitleFormat(title: String): Either<ScopeValidationServiceError.TitleValidationError, Unit> {
        val trimmedTitle = title.trim()
        
        // Check for empty title
        if (trimmedTitle.isBlank()) {
            return ScopeValidationServiceError.TitleValidationError.EmptyTitle.left()
        }
        
        // Check minimum length
        if (trimmedTitle.length < ScopeTitle.MIN_LENGTH) {
            return ScopeValidationServiceError.TitleValidationError.TooShort(
                minLength = ScopeTitle.MIN_LENGTH,
                actualLength = trimmedTitle.length
            ).left()
        }
        
        // Check maximum length using service-specific constant
        if (trimmedTitle.length > MAX_TITLE_LENGTH) {
            return ScopeValidationServiceError.TitleValidationError.TooLong(
                maxLength = MAX_TITLE_LENGTH,
                actualLength = trimmedTitle.length
            ).left()
        }
        
        // Check for invalid characters
        if (title.contains('\n') || title.contains('\r')) {
            val invalidChars = title.filter { it == '\n' || it == '\r' }.toSet()
            return ScopeValidationServiceError.TitleValidationError.InvalidCharacters(invalidChars).left()
        }
        
        return Unit.right()
    }

    /**
     * Validates hierarchy constraints with business rule-specific error context.
     * Returns specific BusinessRuleServiceError.ScopeBusinessRuleError types.
     */
    suspend fun validateHierarchyConstraints(parentId: ScopeId?): Either<BusinessRuleServiceError.ScopeBusinessRuleError, Unit> = either {
        if (parentId == null) {
            return@either
        }

        // Check depth constraint
        val depth = repository.findHierarchyDepth(parentId)
            .mapLeft { repositoryError -> 
                BusinessRuleServiceError.ScopeBusinessRuleError.CheckFailed(
                    checkName = "hierarchy_depth_check",
                    errorDetails = "Failed to retrieve hierarchy depth for validation",
                    affectedScopeId = parentId,
                    cause = RuntimeException("Repository error during depth check: $repositoryError")
                )
            }
            .bind()

        if (depth >= MAX_HIERARCHY_DEPTH) {
            raise(BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded(
                maxDepth = MAX_HIERARCHY_DEPTH,
                attemptedDepth = depth + 1,
                affectedScopeId = parentId
            ))
        }

        // Check children limit constraint
        val childrenCount = repository.countByParentId(parentId)
            .mapLeft { repositoryError ->
                BusinessRuleServiceError.ScopeBusinessRuleError.CheckFailed(
                    checkName = "children_count_check",
                    errorDetails = "Failed to retrieve children count for validation",
                    affectedScopeId = parentId,
                    cause = RuntimeException("Repository error during children count check: $repositoryError")
                )
            }
            .bind()

        if (childrenCount >= MAX_CHILDREN_PER_PARENT) {
            raise(BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded(
                maxChildren = MAX_CHILDREN_PER_PARENT,
                currentChildren = childrenCount,
                parentId = parentId
            ))
        }
    }

    /**
     * Validates title uniqueness with service-specific error context.
     * Returns specific ScopeValidationServiceError.UniquenessValidationError types.
     */
    suspend fun validateTitleUniquenessTyped(
        title: String, 
        parentId: ScopeId?
    ): Either<ScopeValidationServiceError.UniquenessValidationError, Unit> = either {
        val normalizedTitle = TitleNormalizer.normalize(title)
        val duplicateExists = repository.existsByParentIdAndTitle(parentId, normalizedTitle)
            .mapLeft { repositoryError ->
                // Map repository error to check failure, not duplicate title
                ScopeValidationServiceError.UniquenessValidationError.CheckFailed(
                    checkName = "title_uniqueness_check",
                    errorDetails = "Failed to check title uniqueness in repository",
                    title = title,
                    parentId = parentId,
                    cause = RuntimeException("Repository error during uniqueness check: $repositoryError")
                )
            }
            .bind()

        if (duplicateExists) {
            raise(ScopeValidationServiceError.UniquenessValidationError.DuplicateTitle(
                title = title,
                parentId = parentId,
                normalizedTitle = normalizedTitle
            ))
        }
    }

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
            .mapLeft { findError -> 
                // Map FindScopeError to appropriate DomainError
                when (findError) {
                    is io.github.kamiazya.scopes.domain.error.FindScopeError.CircularReference ->
                        DomainError.ScopeError.CircularReference(findError.scopeId, parentId)
                    is io.github.kamiazya.scopes.domain.error.FindScopeError.OrphanedScope ->
                        DomainError.ScopeError.InvalidParent(parentId, findError.message)
                    else -> DomainError.ScopeError.InvalidParent(parentId, "Unable to determine hierarchy depth")
                }
            }
            .bind()

        ensure(depth < MAX_HIERARCHY_DEPTH) {
            DomainError.ScopeError.InvalidParent(parentId, "Maximum hierarchy depth ($MAX_HIERARCHY_DEPTH) would be exceeded")
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
            .mapLeft { countError ->
                // Map CountScopeError to appropriate DomainError
                when (countError) {
                    is io.github.kamiazya.scopes.domain.error.CountScopeError.InvalidParentId ->
                        DomainError.ScopeError.InvalidParent(parentId, countError.message)
                    else -> DomainError.ScopeError.InvalidParent(parentId, "Unable to count children")
                }
            }
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
        val normalizedTitle = TitleNormalizer.normalize(title)
        val duplicateExists = repository.existsByParentIdAndTitle(parentId, normalizedTitle)
            .mapLeft { existsError ->
                // Map ExistsScopeError to appropriate DomainError
                when (existsError) {
                    is io.github.kamiazya.scopes.domain.error.ExistsScopeError.IndexCorruption ->
                        DomainError.ScopeError.InvalidParent(parentId ?: ScopeId.generate(), existsError.message)
                    is io.github.kamiazya.scopes.domain.error.ExistsScopeError.QueryTimeout ->
                        DomainError.InfrastructureError(RepositoryError.DatabaseError("Query timeout during existence check", RuntimeException(existsError.toString())))
                    is io.github.kamiazya.scopes.domain.error.ExistsScopeError.LockTimeout ->
                        DomainError.InfrastructureError(RepositoryError.DatabaseError("Lock timeout during existence check", RuntimeException(existsError.toString())))
                    is io.github.kamiazya.scopes.domain.error.ExistsScopeError.ConnectionFailure ->
                        DomainError.InfrastructureError(RepositoryError.ConnectionError(existsError.cause))
                    is io.github.kamiazya.scopes.domain.error.ExistsScopeError.PersistenceError ->
                        DomainError.InfrastructureError(RepositoryError.DatabaseError(existsError.message, existsError.cause))
                    is io.github.kamiazya.scopes.domain.error.ExistsScopeError.UnknownError ->
                        DomainError.InfrastructureError(RepositoryError.UnknownError(existsError.message, existsError.cause))
                }
            }
            .bind()

        ensure(!duplicateExists) {
            DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle(title, parentId)
        }
    }
}
