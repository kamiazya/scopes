package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.application.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Cross-aggregate validation service for validations that span multiple aggregates.
 *
 * This service handles validation logic that requires coordination between different
 * aggregates and bounded contexts, following DDD principles:
 * - Eventual consistency patterns
 * - Saga pattern for distributed validation
 * - Cross-aggregate invariant enforcement
 * - Compensation patterns for distributed systems
 *
 * Based on DDD principles, this service maintains aggregate boundaries while
 * providing validation coordination across them.
 */
class CrossAggregateValidationService(private val scopeRepository: ScopeRepository) {

    /**
     * Validates hierarchy consistency across multiple aggregates.
     * Ensures that parent-child relationships are consistent and all referenced
     * aggregates exist and are in valid states.
     */
    suspend fun validateHierarchyConsistency(parentId: ScopeId, childIds: List<ScopeId>): Either<CrossAggregateValidationError, Unit> = either {
        // Validate parent exists
        val parentExists = scopeRepository.existsById(parentId)
            .mapLeft {
                CrossAggregateValidationError.CrossReferenceViolation(
                    sourceAggregate = "children",
                    targetAggregate = parentId.value,
                    referenceType = "parentId",
                    violation = "Failed to verify parent scope",
                )
            }
            .bind()

        ensure(parentExists) {
            CrossAggregateValidationError.CrossReferenceViolation(
                sourceAggregate = "children",
                targetAggregate = parentId.value,
                referenceType = "parentId",
                violation = "Parent scope does not exist",
            )
        }

        // Validate all children exist
        childIds.forEach { childId ->
            val childExists = scopeRepository.existsById(childId)
                .mapLeft {
                    CrossAggregateValidationError.CrossReferenceViolation(
                        sourceAggregate = "parentId",
                        targetAggregate = childId.value,
                        referenceType = "childId",
                        violation = "Failed to verify child scope",
                    )
                }
                .bind()

            ensure(childExists) {
                CrossAggregateValidationError.CrossReferenceViolation(
                    sourceAggregate = "parentId",
                    targetAggregate = childId.value,
                    referenceType = "childId",
                    violation = "Child scope does not exist",
                )
            }
        }
    }

    /**
     * Validates uniqueness constraints across multiple aggregate contexts.
     * Ensures that titles are unique across all specified context aggregates,
     * supporting distributed uniqueness invariants.
     */
    suspend fun validateCrossAggregateUniqueness(title: String, contextIds: List<ScopeId>): Either<CrossAggregateValidationError, Unit> = either {
        // Check uniqueness across all contexts
        // Repository will handle title normalization internally
        contextIds.forEach { contextId ->
            val existsInContext = scopeRepository.existsByParentIdAndTitle(contextId, title)
                .mapLeft {
                    CrossAggregateValidationError.InvariantViolation(
                        invariantName = "crossAggregateUniqueness",
                        aggregateIds = contextIds.map { it.value },
                        violationDescription = "Cross-aggregate uniqueness check failed",
                    )
                }
                .bind()

            ensure(!existsInContext) {
                CrossAggregateValidationError.InvariantViolation(
                    invariantName = "titleUniqueness",
                    aggregateIds = listOf(contextId.value),
                    violationDescription = "Title '$title' already exists in context ${contextId.value}",
                )
            }
        }
    }

    /**
     * Validates that circular references do not exist in the hierarchy.
     * This is a cross-aggregate concern as it requires traversing multiple aggregates.
     */
    suspend fun validateNoCircularReferences(scopeId: ScopeId, newParentId: ScopeId?): Either<CrossAggregateValidationError, Unit> = either {
        if (newParentId == null) return@either

        // Check if setting this parent would create a cycle
        var currentId: ScopeId? = newParentId
        val visited = mutableSetOf<ScopeId>()

        while (currentId != null) {
            ensure(currentId != scopeId) {
                CrossAggregateValidationError.InvariantViolation(
                    invariantName = "noCircularReferences",
                    aggregateIds = listOf(scopeId.value, newParentId.value),
                    violationDescription = "Setting parent would create a circular reference",
                )
            }

            ensure(currentId !in visited) {
                CrossAggregateValidationError.InvariantViolation(
                    invariantName = "noCircularReferences",
                    aggregateIds = visited.map { it.value } + currentId.value,
                    violationDescription = "Circular reference detected in hierarchy",
                )
            }

            visited.add(currentId)

            // Get parent of current scope
            val scope = scopeRepository.findById(currentId)
                .mapLeft {
                    CrossAggregateValidationError.CrossReferenceViolation(
                        sourceAggregate = scopeId.value,
                        targetAggregate = currentId.value,
                        referenceType = "hierarchyTraversal",
                        violation = "Failed to load scope during hierarchy validation",
                    )
                }
                .bind()

            currentId = scope?.parentId
        }
    }
}
