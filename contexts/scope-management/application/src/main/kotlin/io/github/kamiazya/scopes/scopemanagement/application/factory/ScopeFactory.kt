package io.github.kamiazya.scopes.scopemanagement.application.factory

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.domain.error.currentTimestamp
import io.github.kamiazya.scopes.platform.observability.Loggable
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.error.AvailabilityReason
import io.github.kamiazya.scopes.scopemanagement.domain.error.HierarchyOperation
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle

/**
 * Factory for creating Scope aggregates with complex validation logic.
 *
 * This factory encapsulates the complexity of creating a new Scope,
 * including hierarchy validation, uniqueness checks, and policy enforcement.
 * Following DDD principles, it keeps the aggregate creation logic
 * within the domain layer while handling cross-aggregate concerns.
 *
 * Key responsibilities:
 * - Validate hierarchy constraints before creation
 * - Check title uniqueness within the same parent scope
 * - Enforce hierarchy policies (depth and children limits)
 * - Coordinate with domain services for complex validations
 */
class ScopeFactory(
    private val scopeRepository: ScopeRepository,
    private val hierarchyApplicationService: ScopeHierarchyApplicationService,
    private val hierarchyService: ScopeHierarchyService,
) {

    companion object : Loggable {
        /**
         * Maps persistence errors to domain-specific hierarchy errors.
         * Logs technical details while returning business-meaningful errors.
         */
        private fun mapPersistenceError(error: PersistenceError, operation: HierarchyOperation, scopeId: ScopeId? = null): ScopeHierarchyError {
            // Log technical details for debugging
            logger.debug(
                "Factory operation failed",
                mapOf(
                    "operation" to operation.name,
                    "scopeId" to (scopeId?.value ?: "null"),
                    "error" to error.toString(),
                ),
            )

            // Map to business-meaningful error
            val reason = when (error) {
                is PersistenceError.StorageUnavailable -> AvailabilityReason.TEMPORARILY_UNAVAILABLE
                is PersistenceError.DataCorruption -> AvailabilityReason.CORRUPTED_HIERARCHY
                is PersistenceError.ConcurrencyConflict -> AvailabilityReason.CONCURRENT_MODIFICATION
                is PersistenceError.NotFound -> AvailabilityReason.TEMPORARILY_UNAVAILABLE
            }

            return ScopeHierarchyError.HierarchyUnavailable(
                occurredAt = currentTimestamp(),
                scopeId = scopeId,
                operation = operation,
                reason = reason,
            )
        }
    }

    /**
     * Creates a new Scope aggregate with full validation.
     *
     * @param title The title of the new scope
     * @param description Optional description
     * @param parentId Optional parent scope ID
     * @param hierarchyPolicy The hierarchy policy to enforce
     * @return Either an error or the created ScopeAggregate
     */
    suspend fun createScope(
        title: String,
        description: String? = null,
        parentId: ScopeId? = null,
        hierarchyPolicy: HierarchyPolicy,
    ): Either<ScopesError, ScopeAggregate> = either {
        // First, validate the title format
        val validatedTitle = ScopeTitle.create(title).bind()

        // Generate the scope ID early so we can use it in error messages
        val newScopeId = ScopeId.generate()

        // If parent is specified, validate hierarchy constraints
        if (parentId != null) {
            // Validate parent exists
            val parentExists = scopeRepository.existsById(parentId)
                .mapLeft { error ->
                    mapPersistenceError(error, HierarchyOperation.VERIFY_EXISTENCE, parentId)
                }
                .bind()
            ensure(parentExists) {
                ScopeHierarchyError.ParentNotFound(
                    occurredAt = currentTimestamp(),
                    scopeId = newScopeId,
                    parentId = parentId,
                )
            }

            // Calculate current hierarchy depth using application service
            val currentDepth = hierarchyApplicationService.calculateHierarchyDepth(parentId).bind()

            // Validate depth limit using pure domain service
            hierarchyService.validateHierarchyDepth(
                newScopeId,
                currentDepth,
                hierarchyPolicy.maxDepth,
            ).bind()

            // Get existing children count
            val existingChildren = scopeRepository.findByParentId(parentId, offset = 0, limit = 1000)
                .mapLeft { error ->
                    mapPersistenceError(error, HierarchyOperation.COUNT_CHILDREN, parentId)
                }
                .bind()

            // Validate children limit
            hierarchyService.validateChildrenLimit(
                parentId,
                existingChildren.size,
                hierarchyPolicy.maxChildrenPerScope,
            ).bind()
        }

        // Check title uniqueness at the same level
        val existingScopeId = scopeRepository.findIdByParentIdAndTitle(
            parentId,
            validatedTitle.value,
        ).mapLeft { error ->
            mapPersistenceError(error, HierarchyOperation.VERIFY_EXISTENCE, parentId)
        }.bind()

        ensure(existingScopeId == null) {
            ScopeUniquenessError.DuplicateTitle(
                occurredAt = currentTimestamp(),
                title = title,
                parentScopeId = parentId,
                existingScopeId = existingScopeId!!,
            )
        }

        // Create the aggregate using the factory method with the pre-generated ID
        ScopeAggregate.create(
            title = title,
            description = description,
            parentId = parentId,
            scopeId = newScopeId,
        ).bind()
    }
}
