package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.UseCaseResult
import io.github.kamiazya.scopes.application.usecase.isErr
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Handler for CreateScope command.
 * Implements UseCase interface following DDD and Clean Architecture principles.
 * 
 * Orchestrates domain logic without containing business rules.
 * All invariants are enforced in domain layer.
 * One handler = one transaction boundary.
 */
class CreateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val applicationScopeValidationService: ApplicationScopeValidationService
) : UseCase<CreateScope, UseCaseResult<CreateScopeResult>> {

    @Suppress("ReturnCount") // Early returns improve readability in result handling
    override suspend operator fun invoke(input: CreateScope): UseCaseResult<CreateScopeResult> {
        // Transaction boundary starts here (simulated with comment)
        // In real implementation, this would be wrapped in @Transactional

        // Step 1: Parse and validate parent exists (returns parsed ScopeId)
        val parentIdResult = validateParentExists(input.parentId)
        if (parentIdResult.isErr()) {
            return UseCaseResult.err((parentIdResult as UseCaseResult.Err).error)
        }
        val parentId = (parentIdResult as UseCaseResult.Ok).value

        // Step 2: Perform repository-dependent validations
        // (hierarchy depth, children limit, title uniqueness)
        val validationResult = validateScopeCreation(input.title, input.description, parentId)
        if (validationResult.isErr()) {
            return UseCaseResult.err((validationResult as UseCaseResult.Err).error)
        }

        // Step 3: Create domain entity (enforces domain invariants)
        val scopeResult = createScopeEntity(input.title, input.description, parentId, input.metadata)
        if (scopeResult.isErr()) {
            return UseCaseResult.err((scopeResult as UseCaseResult.Err).error)
        }
        val scope = (scopeResult as UseCaseResult.Ok).value

        // Step 4: Persist the entity
        val savedScopeResult = saveScopeEntity(scope)
        if (savedScopeResult.isErr()) {
            return UseCaseResult.err((savedScopeResult as UseCaseResult.Err).error)
        }
        val savedScope = (savedScopeResult as UseCaseResult.Ok).value

        // Step 5: Map to DTO (no domain entities leak out)
        return UseCaseResult.ok(ScopeMapper.toCreateScopeResult(savedScope))
    }

    /**
     * Validates that parent scope exists if parentId is provided.
     * Returns the parsed ScopeId if valid, null if not provided.
     */
    @Suppress("ReturnCount") // Early returns improve readability in result handling
    private suspend fun validateParentExists(parentIdString: String?): UseCaseResult<ScopeId?> {
        if (parentIdString == null) {
            return UseCaseResult.ok(null)
        }

        val parentId = try {
            ScopeId.from(parentIdString)
        } catch (e: IllegalArgumentException) {
            return UseCaseResult.err(
                ApplicationError.DomainErrors.single(
                    io.github.kamiazya.scopes.domain.error.DomainError.ScopeError.ScopeNotFound
                )
            )
        }

        return scopeRepository.existsById(parentId)
            .fold(
                ifLeft = { error -> UseCaseResult.err(ApplicationError.Repository(error)) },
                ifRight = { exists ->
                    if (!exists) {
                        UseCaseResult.err(
                            ApplicationError.DomainErrors.single(
                                io.github.kamiazya.scopes.domain.error.DomainError.ScopeError.ScopeNotFound
                            )
                        )
                    } else {
                        UseCaseResult.ok(parentId)
                    }
                }
            )
    }

    /**
     * Validates repository-dependent business rules.
     * These cannot be checked in Scope.create as they require repository access.
     */
    private suspend fun validateScopeCreation(
        title: String, 
        description: String?, 
        parentId: ScopeId?
    ): UseCaseResult<Unit> {
        val validationResult = applicationScopeValidationService.validateScopeCreation(
            title,
            description,
            parentId
        )

        return ApplicationError.fromValidationResult(validationResult).fold(
            ifLeft = { error -> UseCaseResult.err(error) },
            ifRight = { value -> UseCaseResult.ok(value) }
        )
    }

    /**
     * Creates domain entity using safe factory method.
     * Domain layer enforces all business invariants.
     */
    private fun createScopeEntity(
        title: String,
        description: String?,
        parentId: ScopeId?,
        metadata: Map<String, String>
    ): UseCaseResult<Scope> {
        return Scope.create(
            title = title,
            description = description,
            parentId = parentId,
            metadata = metadata
        ).fold(
            ifLeft = { error -> UseCaseResult.err(ApplicationError.DomainErrors.single(error)) },
            ifRight = { scope -> UseCaseResult.ok(scope) }
        )
    }

    /**
     * Persists the entity through repository.
     */
    private suspend fun saveScopeEntity(scope: Scope): UseCaseResult<Scope> =
        scopeRepository.save(scope).fold(
            ifLeft = { error -> UseCaseResult.err(ApplicationError.Repository(error)) },
            ifRight = { savedScope -> UseCaseResult.ok(savedScope) }
        )

}
