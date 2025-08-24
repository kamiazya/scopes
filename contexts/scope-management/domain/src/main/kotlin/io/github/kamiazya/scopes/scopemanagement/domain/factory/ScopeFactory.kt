package io.github.kamiazya.scopes.scopemanagement.domain.factory

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
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
class ScopeFactory(private val hierarchyService: ScopeHierarchyService, private val scopeRepository: ScopeRepository) {
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

        // If parent is specified, validate hierarchy constraints
        if (parentId != null) {
            // Validate parent exists
            val parentExists = scopeRepository.existsById(parentId).bind()
            ensure(parentExists) {
                ScopeHierarchyError.ParentNotFound(
                    occurredAt = Clock.System.now(),
                    scopeId = parentId,
                    parentId = parentId,
                )
            }

            // Calculate current hierarchy depth
            val currentDepth = hierarchyService.calculateHierarchyDepth(
                parentId,
            ) { id -> scopeRepository.findById(id).getOrNull() }.bind()

            // Validate depth limit
            hierarchyService.validateHierarchyDepth(
                parentId,
                currentDepth,
                hierarchyPolicy.maxDepth,
            ).bind()

            // Get existing children count
            val existingChildren = scopeRepository.findByParentId(parentId).bind()

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
        ).bind()

        ensure(existingScopeId == null) {
            ScopeUniquenessError.DuplicateTitle(
                occurredAt = Clock.System.now(),
                title = title,
                parentScopeId = parentId,
                existingScopeId = existingScopeId!!,
            )
        }

        // Create the aggregate using the factory method
        ScopeAggregate.create(
            title = title,
            description = description,
            parentId = parentId,
        ).bind()
    }
}
