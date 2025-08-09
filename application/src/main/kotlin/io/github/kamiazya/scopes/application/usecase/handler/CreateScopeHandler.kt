package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.application.usecase.UseCase
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
) : UseCase<CreateScope, Either<ApplicationError, CreateScopeResult>> {

    override suspend operator fun invoke(input: CreateScope): Either<ApplicationError, CreateScopeResult> = either {
        // Transaction boundary starts here (simulated with comment)
        // In real implementation, this would be wrapped in @Transactional

        // Step 1: Check parent exists (prerequisite for other validations)
        validateParentExists(input.parentId).bind()

        // Step 2: Perform repository-dependent validations
        // (hierarchy depth, children limit, title uniqueness)
        validateScopeCreation(input).bind()

        // Step 3: Create domain entity (enforces domain invariants)
        val scope = createScopeEntity(input).bind()

        // Step 4: Persist the entity
        val savedScope = saveScopeEntity(scope).bind()

        // Step 5: Map to DTO (no domain entities leak out)
        ScopeMapper.toCreateScopeResult(savedScope)
    }

    /**
     * Validates that parent scope exists if parentId is provided.
     */
    private suspend fun validateParentExists(parentId: ScopeId?): Either<ApplicationError, Unit> = either {
        if (parentId == null) {
            return@either
        }

        val exists = scopeRepository.existsById(parentId)
            .mapLeft { ApplicationError.Repository(it) }
            .bind()

        if (!exists) {
            raise(
                ApplicationError.DomainErrors.single(
                    io.github.kamiazya.scopes.domain.error.DomainError.ScopeError.ScopeNotFound
                )
            )
        }
    }

    /**
     * Validates repository-dependent business rules.
     * These cannot be checked in Scope.create as they require repository access.
     */
    private suspend fun validateScopeCreation(input: CreateScope): Either<ApplicationError, Unit> {
        val validationResult = applicationScopeValidationService.validateScopeCreation(
            input.title,
            input.description,
            input.parentId
        )

        return ApplicationError.fromValidationResult(validationResult)
    }

    /**
     * Creates domain entity using safe factory method.
     * Domain layer enforces all business invariants.
     */
    private fun createScopeEntity(input: CreateScope): Either<ApplicationError, Scope> = either {
        Scope.create(
            title = input.title,
            description = input.description,
            parentId = input.parentId,
            metadata = input.metadata
        ).mapLeft { ApplicationError.DomainErrors.single(it) }
            .bind()
    }

    /**
     * Persists the entity through repository.
     */
    private suspend fun saveScopeEntity(scope: Scope): Either<ApplicationError, Scope> =
        scopeRepository.save(scope)
            .mapLeft { ApplicationError.Repository(it) }

}
