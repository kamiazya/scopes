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

    @Suppress("ReturnCount") // Early returns improve readability in result handling
    override suspend operator fun invoke(input: CreateScope): Either<ApplicationError, CreateScopeResult> = either {
        // Transaction boundary starts here (simulated with comment)
        // In real implementation, this would be wrapped in @Transactional

        // Step 1: Parse and validate parent exists (returns parsed ScopeId)
        val parentId = validateParentExists(input.parentId).bind()

        // Step 2: Perform repository-dependent validations
        // (hierarchy depth, children limit, title uniqueness)
        validateScopeCreation(input.title, input.description, parentId).bind()

        // Step 3: Create domain entity (enforces domain invariants)
        val scope = createScopeEntity(input.title, input.description, parentId, input.metadata).bind()

        // Step 4: Persist the entity
        val savedScope = saveScopeEntity(scope).bind()

        // Step 5: Map to DTO (no domain entities leak out)
        ScopeMapper.toCreateScopeResult(savedScope)
    }

    /**
     * Validates that parent scope exists if parentId is provided.
     * Returns the parsed ScopeId if valid, null if not provided.
     */
    @Suppress("ReturnCount") // Early returns improve readability in result handling
    private suspend fun validateParentExists(parentIdString: String?): Either<ApplicationError, ScopeId?> = either {
        if (parentIdString == null) {
            return@either null
        }

        val parentId = try {
            ScopeId.from(parentIdString)
        } catch (e: IllegalArgumentException) {
            raise(
                ApplicationError.DomainErrors.single(
                    io.github.kamiazya.scopes.domain.error.DomainError.ScopeError.ScopeNotFound
                )
            )
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

        parentId
    }

    /**
     * Validates repository-dependent business rules.
     * These cannot be checked in Scope.create as they require repository access.
     */
    private suspend fun validateScopeCreation(
        title: String, 
        description: String?, 
        parentId: ScopeId?
    ): Either<ApplicationError, Unit> {
        val validationResult = applicationScopeValidationService.validateScopeCreation(
            title,
            description,
            parentId
        )

        return ApplicationError.fromValidationResult(validationResult)
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
    ): Either<ApplicationError, Scope> =
        Scope.create(
            title = title,
            description = description,
            parentId = parentId,
            metadata = metadata
        ).mapLeft { ApplicationError.DomainErrors.single(it) }

    /**
     * Persists the entity through repository.
     */
    private suspend fun saveScopeEntity(scope: Scope): Either<ApplicationError, Scope> =
        scopeRepository.save(scope)
            .mapLeft { ApplicationError.Repository(it) }

}
