package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.service.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.util.TitleNormalizer
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
class CrossAggregateValidationService(
    private val scopeRepository: ScopeRepository
) {

    /**
     * Validates hierarchy consistency across multiple aggregates.
     * Ensures that parent-child relationships are consistent and all referenced
     * aggregates exist and are in valid states.
     */
    suspend fun validateHierarchyConsistency(
        parentId: ScopeId,
        childIds: List<ScopeId>
    ): Either<CrossAggregateValidationError, Unit> = either {

        // Validate parent exists
        val parentExists = scopeRepository.existsById(parentId)
            .mapLeft { existsError ->
                CrossAggregateValidationError.CrossReferenceViolation(
                    sourceAggregate = "children",
                    targetAggregate = parentId.value,
                    referenceType = "parentId",
                    violation = "Failed to verify parent scope"
                )
            }
            .bind()

        if (!parentExists) {
            raise(CrossAggregateValidationError.CrossReferenceViolation(
                sourceAggregate = "children",
                targetAggregate = parentId.value,
                referenceType = "parentId",
                violation = "Parent scope does not exist"
            ))
        }

        // Validate all children exist
        for (childId in childIds) {
            val childExists = scopeRepository.existsById(childId)
                .mapLeft { existsError ->
                    CrossAggregateValidationError.CrossReferenceViolation(
                        sourceAggregate = "parentId",
                        targetAggregate = childId.value,
                        referenceType = "childId",
                        violation = "Failed to verify child scope"
                    )
                }
                .bind()

            if (!childExists) {
                raise(CrossAggregateValidationError.CrossReferenceViolation(
                    sourceAggregate = "parentId",
                    targetAggregate = childId.value,
                    referenceType = "childId",
                    violation = "Child scope does not exist"
                ))
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
        contextIds: List<ScopeId>
    ): Either<CrossAggregateValidationError, Unit> = either {

        val normalizedTitle = TitleNormalizer.normalize(title)

        // Check uniqueness across all contexts
        for (contextId in contextIds) {
            val existsInContext = scopeRepository.existsByParentIdAndTitle(contextId, normalizedTitle)
                .mapLeft { existsError ->
                    CrossAggregateValidationError.InvariantViolation(
                        invariantName = "crossAggregateUniqueness",
                        aggregateIds = contextIds.map { it.value },
                        violationDescription = "Cross-aggregate uniqueness check failed"
                    )
                }
                .bind()

            if (existsInContext) {
                raise(CrossAggregateValidationError.InvariantViolation(
                    invariantName = "crossAggregateUniqueness",
                    aggregateIds = contextIds.map { it.value },
                    violationDescription = "Title '$title' conflicts across aggregates"
                ))
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
        consistencyRule: String
    ): Either<CrossAggregateValidationError, Unit> = either {

        // Validate all aggregates exist and are in valid state
        for (aggregateIdString in aggregateIds) {
            val aggregateId = try {
                ScopeId.from(aggregateIdString)
            } catch (e: IllegalArgumentException) {
                raise(CrossAggregateValidationError.AggregateConsistencyViolation(
                    operation = operation,
                    affectedAggregates = aggregateIds,
                    consistencyRule = consistencyRule,
                    violationDetails = "Invalid aggregate ID format: $aggregateIdString"
                ))
            }

            val aggregateExists = scopeRepository.existsById(aggregateId)
                .mapLeft { existsError ->
                    CrossAggregateValidationError.AggregateConsistencyViolation(
                        operation = operation,
                        affectedAggregates = aggregateIds,
                        consistencyRule = consistencyRule,
                        violationDetails = "Failed to verify aggregate consistency"
                    )
                }
                .bind()

            if (!aggregateExists) {
                raise(CrossAggregateValidationError.AggregateConsistencyViolation(
                    operation = operation,
                    affectedAggregates = aggregateIds,
                    consistencyRule = consistencyRule,
                    violationDetails = "Aggregate $aggregateIdString does not exist or is in invalid state"
                ))
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
        operation: String
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
                raise(CrossAggregateValidationError.InvariantViolation(
                    invariantName = ruleName,
                    aggregateIds = aggregateStates.keys.toList(),
                    violationDescription = "Unknown distributed business rule: $ruleName"
                ))
            }
        }
    }
}
