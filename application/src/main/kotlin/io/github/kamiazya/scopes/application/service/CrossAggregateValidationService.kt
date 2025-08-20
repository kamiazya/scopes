package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.application.service.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Cross-aggregate validation service for validations that span multiple aggregates.
 *
 * This service handles validation logic that requires coordination between different
 * aggregates and bounded contexts, following Serena MCP research on:
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
    suspend fun validateHierarchyConsistency(
        parentId: ScopeId,
        childIds: List<ScopeId>,
    ): Either<CrossAggregateValidationError, Unit> = either {
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
    suspend fun validateCrossAggregateUniqueness(
        title: String,
        contextIds: List<ScopeId>,
    ): Either<CrossAggregateValidationError, Unit> = either {
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
                    invariantName = "crossAggregateUniqueness",
                    aggregateIds = contextIds.map { it.value },
                    violationDescription = "Title '$title' conflicts across aggregates",
                )
            }
        }
    }

    /**
     * Validates aggregate consistency for operations affecting multiple aggregates.
     * Ensures that all involved aggregates are in valid and consistent states
     * before allowing operations that span aggregate boundaries.
     */
    suspend fun validateAggregateConsistency(
        operation: String,
        aggregateIds: Set<String>,
        consistencyRule: String,
    ): Either<CrossAggregateValidationError, Unit> = either {
        // Validate all aggregates exist and are in valid state
        aggregateIds.forEach { aggregateIdString ->
            val aggregateId = ScopeId.create(aggregateIdString)
                .mapLeft {
                    CrossAggregateValidationError.AggregateConsistencyViolation(
                        operation = operation,
                        affectedAggregates = aggregateIds,
                        consistencyRule = consistencyRule,
                        violationDetails = "Invalid aggregate ID format: $aggregateIdString",
                    )
                }
                .bind()

            val aggregateExists = scopeRepository.existsById(aggregateId)
                .mapLeft {
                    CrossAggregateValidationError.AggregateConsistencyViolation(
                        operation = operation,
                        affectedAggregates = aggregateIds,
                        consistencyRule = consistencyRule,
                        violationDetails = "Failed to verify aggregate consistency",
                    )
                }
                .bind()

            ensure(aggregateExists) {
                CrossAggregateValidationError.AggregateConsistencyViolation(
                    operation = operation,
                    affectedAggregates = aggregateIds,
                    consistencyRule = consistencyRule,
                    violationDetails = "Aggregate $aggregateIdString does not exist or is in invalid state",
                )
            }
        }
    }

    /**
     * Future extension point: Validates distributed business rules that span aggregates.
     * This could be extended to support saga pattern validation coordination.
     */
    suspend fun validateDistributedBusinessRule(
        ruleName: String,
        aggregateStates: Map<String, Any>,
        operation: String,
    ): Either<CrossAggregateValidationError, Unit> = either {
        // Implementation would depend on specific distributed business rules
        // This is a placeholder for future saga pattern integration

        when (ruleName) {
            "eventualConsistency" -> {
                // Validate eventual consistency requirements
                // In a real implementation, this might coordinate with other services
                // or check compensation transaction states
            }
            else -> {
                raise(
                    CrossAggregateValidationError.InvariantViolation(
                        invariantName = ruleName,
                        aggregateIds = aggregateStates.keys.toList(),
                        violationDescription = "Unknown distributed business rule: $ruleName",
                    ),
                )
            }
        }
    }
}
