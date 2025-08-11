package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.*
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.util.TitleNormalizer
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

    // ===== NEW SERVICE-SPECIFIC ERROR METHODS =====

    /**
     * Validates title format with service-specific error context.
     * Returns specific ScopeValidationServiceError.TitleValidationError types.
     * Delegates to ScopeTitle.create() and maps domain errors to service errors.
     */
    fun validateTitleFormat(title: String): Either<TitleValidationError, Unit> {
        val trimmedTitle = title.trim()

        // Check for empty title
        if (trimmedTitle.isBlank()) {
            return TitleValidationError.EmptyTitle.left()
        }

        // Check minimum length
        if (trimmedTitle.length < ScopeTitle.MIN_LENGTH) {
            return TitleValidationError.TitleTooShort(
                minLength = ScopeTitle.MIN_LENGTH,
                actualLength = trimmedTitle.length,
                title = trimmedTitle
            ).left()
        }

        // Check maximum length using service-specific constant
        if (trimmedTitle.length > ScopeTitle.MAX_LENGTH) {
            return TitleValidationError.TitleTooLong(
                maxLength = ScopeTitle.MAX_LENGTH,
                actualLength = trimmedTitle.length,
                title = trimmedTitle
            ).left()
        }

        // Check for invalid characters
        if (title.contains('\n') || title.contains('\r')) {
            val invalidChars = title.filter { it == '\n' || it == '\r' }.toSet()
            val position = title.indexOfFirst { it == '\n' || it == '\r' }
            return TitleValidationError.InvalidCharacters(
                title = title,
                invalidCharacters = invalidChars,
                position = position
            ).left()
        }

        return Unit.right()
    }

    /**
     * Validates hierarchy constraints with business rule-specific error context.
     * Returns specific BusinessRuleServiceError.ScopeBusinessRuleError types.
     */
    suspend fun validateHierarchyConstraints(parentId: ScopeId?): Either<ScopeBusinessRuleError, Unit> = either {
        if (parentId == null) {
            return@either
        }

        // Check depth constraint
        val depth = repository.findHierarchyDepth(parentId)
            .fold(
                ifLeft = { repositoryError ->
                    // Map repository error to business rule error
                    raise(ScopeBusinessRuleError.MaxDepthExceeded(
                        maxDepth = MAX_HIERARCHY_DEPTH,
                        actualDepth = 0,
                        scopeId = parentId.toString(),
                        parentPath = emptyList()
                    ))
                },
                ifRight = { it }
            )

        if (depth >= MAX_HIERARCHY_DEPTH) {
            raise(ScopeBusinessRuleError.MaxDepthExceeded(
                maxDepth = MAX_HIERARCHY_DEPTH,
                actualDepth = depth + 1,
                scopeId = parentId.toString(),
                parentPath = emptyList() // Would need to fetch parent path if required
            ))
        }

        // Check children limit constraint
        val childrenCount = repository.countByParentId(parentId)
            .fold(
                ifLeft = { repositoryError ->
                    // Map repository error to business rule error
                    raise(ScopeBusinessRuleError.MaxChildrenExceeded(
                        maxChildren = MAX_CHILDREN_PER_PARENT,
                        currentChildren = 0,
                        parentId = parentId.toString(),
                        attemptedOperation = "create_child_scope"
                    ))
                },
                ifRight = { it }
            )

        if (childrenCount >= MAX_CHILDREN_PER_PARENT) {
            raise(ScopeBusinessRuleError.MaxChildrenExceeded(
                maxChildren = MAX_CHILDREN_PER_PARENT,
                currentChildren = childrenCount,
                parentId = parentId.toString(),
                attemptedOperation = "create_child_scope"
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
    ): Either<UniquenessValidationError, Unit> = either {
        val normalizedTitle = TitleNormalizer.normalize(title)
        val duplicateExists = repository.existsByParentIdAndTitle(parentId, normalizedTitle)
            .fold(
                ifLeft = { repositoryError ->
                    // Map repository error to uniqueness error
                    raise(UniquenessValidationError.DuplicateTitle(
                        title = title,
                        normalizedTitle = normalizedTitle,
                        parentId = parentId?.toString(),
                        existingScopeId = ""
                    ))
                },
                ifRight = { it }
            )

        if (duplicateExists) {
            raise(UniquenessValidationError.DuplicateTitle(
                title = title,
                normalizedTitle = normalizedTitle,
                parentId = parentId?.toString(),
                existingScopeId = "" // Would need to fetch actual ID if required
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
     * Private translator function to map ScopeBusinessRuleError to ScopeError.
     * Centralizes the conversion of business rule errors to public scope errors.
     */
    private fun mapBusinessRuleErrorToScopeError(error: ScopeBusinessRuleError, parentId: ScopeId?): ScopeError =
        when (error) {
            is ScopeBusinessRuleError.MaxDepthExceeded ->
                ScopeError.InvalidParent(
                    parentId ?: ScopeId.generate(),
                    "Maximum hierarchy depth (${error.maxDepth}) would be exceeded"
                )
            is ScopeBusinessRuleError.MaxChildrenExceeded ->
                ScopeError.InvalidParent(
                    parentId ?: ScopeId.generate(),
                    "Maximum children limit (${error.maxChildren}) would be exceeded"
                )
            is ScopeBusinessRuleError.DuplicateScope ->
                ScopeError.InvalidParent(
                    parentId ?: ScopeId.generate(),
                    "Duplicate scope detected: ${error.existingScopeId}"
                )
        }

    /**
     * Validate hierarchy depth using repository query.
     * Delegates to validateHierarchyConstraints for depth validation.
     */
    suspend fun validateHierarchyDepth(
        parentId: ScopeId?
    ): Either<DomainError, Unit> = either {
        if (parentId == null) {
            return@either
        }

        // Delegate to validateHierarchyConstraints which handles depth checking
        validateHierarchyConstraints(parentId)
            .mapLeft { businessRuleError ->
                // Map business rule errors to scope errors
                mapBusinessRuleErrorToScopeError(businessRuleError, parentId)
            }
            .bind()
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
                    is CountScopeError.InvalidParentId ->
                        ScopeError.InvalidParent(parentId, countError.message)
                    else -> ScopeError.InvalidParent(parentId, "Unable to count children")
                }
            }
            .bind()

        ensure(childrenCount < MAX_CHILDREN_PER_PARENT) {
            ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(
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
                    is ExistsScopeError.IndexCorruption ->
                        // Handle IndexCorruption without generating misleading ScopeIds when parentId is null
                        if (parentId != null) {
                            ScopeError.InvalidParent(
                                parentId,
                                "Index corruption detected for parent: ${existsError.message}. ScopeId in corruption: ${existsError.scopeId}"
                            )
                        } else {
                            DomainInfrastructureError(
                                RepositoryError.DataIntegrityError(
                                    "Index corruption detected for root-level existence check: ${existsError.message}. Corrupted ScopeId: ${existsError.scopeId}",
                                    cause = RuntimeException("Index corruption in existence validation")
                                )
                            )
                        }
                    is ExistsScopeError.QueryTimeout ->
                        DomainInfrastructureError(
                            RepositoryError.DatabaseError(
                                "Query timeout during existence check: operation='${existsError.operation}', timeout=${existsError.timeoutMs}ms, context=${existsError.context}",
                                RuntimeException("Query timeout: ${existsError.operation}")
                            )
                        )
                    is ExistsScopeError.LockTimeout ->
                        DomainInfrastructureError(
                            RepositoryError.DatabaseError(
                                "Lock timeout during existence check: operation='${existsError.operation}', timeout=${existsError.timeoutMs}ms, retryable=${existsError.retryable}",
                                RuntimeException("Lock timeout: ${existsError.operation}")
                            )
                        )
                    is ExistsScopeError.ConnectionFailure ->
                        DomainInfrastructureError(
                            RepositoryError.ConnectionError(existsError.cause)
                        )
                    is ExistsScopeError.PersistenceError ->
                        DomainInfrastructureError(
                            RepositoryError.DatabaseError(
                                "Persistence error during existence check: context=${existsError.context}, retryable=${existsError.retryable}, errorCode=${existsError.errorCode ?: "none"} - ${existsError.message}",
                                existsError.cause
                            )
                        )
                    is ExistsScopeError.UnknownError ->
                        DomainInfrastructureError(
                            RepositoryError.UnknownError(
                                "Unknown error during existence check: ${existsError.message}",
                                existsError.cause
                            )
                        )
                }
            }
            .bind()

        ensure(!duplicateExists) {
            ScopeBusinessRuleViolation.ScopeDuplicateTitle(title, parentId)
        }
    }
}
