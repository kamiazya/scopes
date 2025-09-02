package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeUniquenessValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Cross-aggregate validation service for validations that span multiple aggregates.
 *
 * This service orchestrates domain services and handles validation logic that requires
 * coordination between different aggregates and bounded contexts, following DDD principles:
 * - Eventual consistency patterns
 * - Saga pattern for distributed validation
 * - Cross-aggregate invariant enforcement
 * - Compensation patterns for distributed systems
 *
 * Based on DDD principles, this service maintains aggregate boundaries while
 * providing validation coordination across them using domain services.
 */
class CrossAggregateValidationService(
    private val hierarchyValidationService: ScopeHierarchyValidationService,
    private val uniquenessValidationService: ScopeUniquenessValidationService,
) {

    /**
     * Validates hierarchy consistency across multiple aggregates.
     * Delegates to domain service for business logic and handles error mapping.
     */
    suspend fun validateHierarchyConsistency(parentId: ScopeId, childIds: List<ScopeId>): Either<CrossAggregateValidationError, Unit> = either {
        hierarchyValidationService.validateHierarchyConsistency(parentId, childIds)
            .mapLeft { domainError ->
                CrossAggregateValidationError.CrossReferenceViolation(
                    sourceAggregate = "hierarchy",
                    targetAggregate = parentId.value,
                    referenceType = "parentChild",
                    violation = domainError.toString(),
                )
            }
            .bind()
    }

    /**
     * Validates uniqueness constraints across multiple aggregate contexts.
     * Delegates to domain service for business logic and handles error mapping.
     */
    suspend fun validateCrossAggregateUniqueness(title: String, contextIds: List<ScopeId>): Either<CrossAggregateValidationError, Unit> = either {
        uniquenessValidationService.validateCrossContextUniqueness(title, contextIds)
            .mapLeft { domainError ->
                CrossAggregateValidationError.InvariantViolation(
                    invariantName = "titleUniqueness",
                    aggregateIds = contextIds.map { it.value },
                    violationDescription = domainError.toString(),
                )
            }
            .bind()
    }

    /**
     * Validates that circular references do not exist in the hierarchy.
     * Delegates to domain service for business logic and handles error mapping.
     */
    suspend fun validateNoCircularReferences(scopeId: ScopeId, newParentId: ScopeId?): Either<CrossAggregateValidationError, Unit> = either {
        hierarchyValidationService.validateNoCircularReferences(scopeId, newParentId)
            .mapLeft { domainError ->
                CrossAggregateValidationError.InvariantViolation(
                    invariantName = "noCircularReferences",
                    aggregateIds = listOfNotNull(scopeId.value, newParentId?.value),
                    violationDescription = domainError.toString(),
                )
            }
            .bind()
    }
}
