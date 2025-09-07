package io.github.kamiazya.scopes.scopemanagement.application.service.validation

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Domain service for validating scope hierarchy constraints.
 *
 * This service encapsulates business rules related to scope hierarchy validation,
 * maintaining domain invariants while being agnostic to application concerns.
 */
class ScopeHierarchyValidationService(private val scopeRepository: ScopeRepository) {

    /**
     * Validates that parent-child relationships are consistent.
     * Ensures that parent and all children exist in the domain.
     *
     * @param parentId The ID of the parent scope
     * @param childIds List of child scope IDs
     * @return Either a domain error or Unit on success
     */
    suspend fun validateHierarchyConsistency(parentId: ScopeId, childIds: List<ScopeId>): Either<ContextError, Unit> = either {
        // Business rule: Parent must exist
        val parentExists = scopeRepository.existsById(parentId)
            .mapLeft { ContextError.InvalidScope(parentId.value, ContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND, occurredAt = Clock.System.now()) }
            .bind()
        ensure(parentExists) {
            ContextError.InvalidScope(
                scopeId = parentId.value,
                errorType = ContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND,
                occurredAt = Clock.System.now(),
            )
        }

        // Business rule: All children must exist
        childIds.forEach { childId ->
            val childExists = scopeRepository.existsById(childId)
                .mapLeft {
                    ContextError.InvalidScope(childId.value, ContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND, occurredAt = Clock.System.now())
                }
                .bind()
            ensure(childExists) {
                ContextError.InvalidScope(
                    scopeId = childId.value,
                    errorType = ContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND,
                    occurredAt = Clock.System.now(),
                )
            }
        }
    }

    /**
     * Validates that no circular references exist in the hierarchy.
     * Prevents infinite loops in parent-child relationships.
     *
     * @param scopeId The scope being moved
     * @param newParentId The new parent (null for root)
     * @return Either a domain error or Unit on success
     */
    suspend fun validateNoCircularReferences(scopeId: ScopeId, newParentId: ScopeId?): Either<ContextError, Unit> = either {
        if (newParentId == null) return@either

        // Business rule: Cannot be parent of itself
        ensure(scopeId != newParentId) {
            ContextError.InvalidHierarchy(
                scopeId = scopeId.value,
                parentId = newParentId.value,
                errorType = ContextError.InvalidHierarchy.InvalidHierarchyType.CIRCULAR_REFERENCE,
                occurredAt = Clock.System.now(),
            )
        }

        // Business rule: Cannot create circular reference by checking ancestry
        val isCircular = checkCircularReference(scopeId, newParentId).bind()
        ensure(!isCircular) {
            ContextError.InvalidHierarchy(
                scopeId = scopeId.value,
                parentId = newParentId.value,
                errorType = ContextError.InvalidHierarchy.InvalidHierarchyType.CIRCULAR_REFERENCE,
                occurredAt = Clock.System.now(),
            )
        }
    }

    /**
     * Checks if making scopeId a child of candidateParentId would create a circular reference.
     */
    private suspend fun checkCircularReference(scopeId: ScopeId, candidateParentId: ScopeId): Either<ContextError, Boolean> = either {
        var currentParentId: ScopeId? = candidateParentId
        val visited = mutableSetOf<ScopeId>()

        while (currentParentId != null) {
            // If we've seen this ID before, we have a cycle
            if (currentParentId in visited) {
                return@either true
            }

            // If we reach the original scope, we have a cycle
            if (currentParentId == scopeId) {
                return@either true
            }

            visited.add(currentParentId)

            // Get the next parent in the chain
            val scope = scopeRepository.findById(currentParentId)
                .mapLeft {
                    ContextError.InvalidScope(
                        currentParentId.value,
                        ContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND,
                        occurredAt = Clock.System.now(),
                    )
                }
                .bind()
            currentParentId = scope?.parentId
        }

        false
    }
}
