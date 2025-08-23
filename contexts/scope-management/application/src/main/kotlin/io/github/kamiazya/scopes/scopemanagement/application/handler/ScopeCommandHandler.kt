package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.DomainEventPublisher
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Command handler for Scope aggregate operations.
 *
 * This handler orchestrates the execution of commands against Scope aggregates,
 * coordinating between the domain layer and infrastructure services.
 * It ensures that all business rules are enforced and events are properly published.
 *
 * Responsibilities:
 * - Load aggregates from repository
 * - Execute commands on aggregates
 * - Persist aggregate changes
 * - Publish domain events
 * - Manage transactions
 */
class ScopeCommandHandler(
    private val scopeRepository: ScopeRepository,
    private val eventPublisher: DomainEventPublisher,
    private val hierarchyService: ScopeHierarchyService,
    private val transactionManager: TransactionManager,
) {
    /**
     * Handles the CreateScope command.
     * Creates a new scope with validation of hierarchy and uniqueness.
     */
    suspend fun handleCreateScope(command: CreateScope): Either<ScopesError, CreateScopeResult> = transactionManager.inTransaction {
        either {
            // Validate parent exists if specified
            if (command.parentId != null) {
                val parentId = ScopeId.create(command.parentId).bind()
                val parentExists = scopeRepository.existsById(parentId).bind()
                ensure(parentExists) {
                    ScopeError.ParentNotFound(parentId)
                }
            }

            // Validate title uniqueness at the same level
            val titleExists = scopeRepository.existsByParentIdAndTitle(
                parentId = command.parentId?.let { ScopeId.create(it).bind() },
                title = command.title,
            ).bind()

            ensure(!titleExists) {
                ScopeError.DuplicateTitle(
                    title = command.title,
                    parentId = command.parentId?.let { ScopeId.create(it).bind() },
                )
            }

            // Create the aggregate
            val aggregate = ScopeAggregate.create(
                title = command.title,
                description = command.description,
                parentId = command.parentId?.let { ScopeId.create(it).bind() },
            ).bind()

            // Save the scope
            val scope = aggregate.scope!!
            scopeRepository.save(scope).bind()

            // Publish events (TODO: implement event tracking)
            // val events = aggregate.getUncommittedEvents()
            // eventPublisher.publishAll(events)

            // Return result
            CreateScopeResult(
                id = scope.id.value,
                title = scope.title.value,
                description = scope.description?.value,
                parentId = scope.parentId?.value,
                createdAt = scope.createdAt,
                aspects = emptyMap(),
            )
        }
    }
}
