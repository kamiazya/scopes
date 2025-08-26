package io.github.kamiazya.scopes.scopemanagement.application.factory

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.datetime.Clock

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
            val parentExistsResult = scopeRepository.existsById(parentId)
            val parentExists = when (parentExistsResult) {
                is Either.Right -> parentExistsResult.value
                is Either.Left -> raise(
                    ScopeHierarchyError.PersistenceFailure(
                        occurredAt = Clock.System.now(),
                        operation = "existsById",
                        scopeId = parentId,
                        cause = parentExistsResult.value,
                    ),
                )
            }
            ensure(parentExists) {
                ScopeHierarchyError.ParentNotFound(
                    occurredAt = Clock.System.now(),
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
            val existingChildrenResult = scopeRepository.findByParentId(parentId)
            val existingChildren = when (existingChildrenResult) {
                is Either.Right -> existingChildrenResult.value
                is Either.Left -> raise(
                    ScopeHierarchyError.PersistenceFailure(
                        occurredAt = Clock.System.now(),
                        operation = "findByParentId",
                        scopeId = parentId,
                        cause = existingChildrenResult.value,
                    ),
                )
            }

            // Validate children limit
            hierarchyService.validateChildrenLimit(
                parentId,
                existingChildren.size,
                hierarchyPolicy.maxChildrenPerScope,
            ).bind()
        }

        // Check title uniqueness at the same level
        val existingScopeIdResult = scopeRepository.findIdByParentIdAndTitle(
            parentId,
            validatedTitle.value,
        )
        val existingScopeId = when (existingScopeIdResult) {
            is Either.Right -> existingScopeIdResult.value
            is Either.Left -> raise(
                ScopeHierarchyError.PersistenceFailure(
                    occurredAt = Clock.System.now(),
                    operation = "findIdByParentIdAndTitle",
                    scopeId = parentId,
                    cause = existingScopeIdResult.value,
                ),
            )
        }

        ensure(existingScopeId == null) {
            ScopeUniquenessError.DuplicateTitle(
                occurredAt = Clock.System.now(),
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
