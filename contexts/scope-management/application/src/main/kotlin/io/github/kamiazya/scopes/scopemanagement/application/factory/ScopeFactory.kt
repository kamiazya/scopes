package io.github.kamiazya.scopes.scopemanagement.application.factory

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.observability.Loggable
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeUniquenessError as AppScopeUniquenessError

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
         * Maps repository errors to application errors.
         * Logs technical details while returning business-meaningful errors.
         */
        private fun mapRepositoryError(error: Any, operation: String): ScopeManagementApplicationError {
            // Log technical details for debugging
            logger.debug(
                "Factory operation failed",
                mapOf(
                    "operation" to operation,
                    "error" to error.toString(),
                ),
            )

            return ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                operation = operation,
                errorCause = error.toString(),
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
    ): Either<ScopeManagementApplicationError, ScopeAggregate> = either {
        // First, validate the title format
        val validatedTitle = ScopeTitle.create(title)
            .mapLeft { it.toGenericApplicationError() }
            .bind()

        // Generate the scope ID early so we can use it in error messages
        val newScopeId = ScopeId.generate()

        // If parent is specified, validate hierarchy constraints
        if (parentId != null) {
            // Validate parent exists
            val parentExists = scopeRepository.existsById(parentId)
                .mapLeft { error ->
                    mapRepositoryError(error, "verify-parent-existence")
                }
                .bind()
            ensure(parentExists) {
                ScopeManagementApplicationError.PersistenceError.NotFound(
                    entityType = "Scope",
                    entityId = parentId.value,
                )
            }

            // Calculate current hierarchy depth using application service
            val currentDepth = hierarchyApplicationService.calculateHierarchyDepth(parentId)
                .mapLeft { it.toGenericApplicationError() }
                .bind()

            // Validate depth limit using pure domain service
            hierarchyService.validateHierarchyDepth(
                newScopeId,
                currentDepth,
                hierarchyPolicy.maxDepth,
            ).mapLeft { it.toGenericApplicationError() }.bind()

            // Get existing children count
            val existingChildren = scopeRepository.findByParentId(parentId, offset = 0, limit = 1000)
                .mapLeft { error ->
                    mapRepositoryError(error, "count-children")
                }
                .bind()

            // Validate children limit
            hierarchyService.validateChildrenLimit(
                parentId,
                existingChildren.size,
                hierarchyPolicy.maxChildrenPerScope,
            ).mapLeft { it.toGenericApplicationError() }.bind()
        }

        // Check title uniqueness at the same level
        val existingScopeId = scopeRepository.findIdByParentIdAndTitle(
            parentId,
            validatedTitle.value,
        ).mapLeft { error ->
            mapRepositoryError(error, "check-title-uniqueness")
        }.bind()

        ensure(existingScopeId == null) {
            AppScopeUniquenessError.DuplicateTitle(
                title = title,
                parentScopeId = parentId?.value,
                existingScopeId = existingScopeId!!.value,
            )
        }

        // Create the aggregate using the factory method with the pre-generated ID
        ScopeAggregate.create(
            title = title,
            description = description,
            parentId = parentId,
            scopeId = newScopeId,
        ).mapLeft { it.toGenericApplicationError() }.bind()
    }
}
