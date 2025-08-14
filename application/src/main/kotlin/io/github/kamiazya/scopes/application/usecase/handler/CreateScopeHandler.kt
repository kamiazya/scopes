package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.application.usecase.error.CreateScopeError
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.TitleValidationError
import io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleError
import io.github.kamiazya.scopes.domain.error.UniquenessValidationError
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError

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
) : UseCase<CreateScope, CreateScopeError, CreateScopeResult> {

    @Suppress("ReturnCount") // Early returns improve readability in result handling
    override suspend operator fun invoke(input: CreateScope): Either<CreateScopeError, CreateScopeResult> = either {
        // Transaction boundary starts here (simulated with comment)
        // In real implementation, this would be wrapped in @Transactional

        // Step 1: Parse and validate parent exists (returns parsed ScopeId)
        val parentId = validateParentExists(input.parentId).bind()

        // Step 2: Perform service-specific validations with type-safe error translation
        validateTitleWithServiceErrors(input.title).bind()
        validateHierarchyWithServiceErrors(parentId).bind()
        validateUniquenessWithServiceErrors(input.title, parentId).bind()

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
    private suspend fun validateParentExists(parentIdString: String?): Either<CreateScopeError, ScopeId?> = either {
        if (parentIdString == null) {
            return@either null
        }

        val parentId = try {
            ScopeId.from(parentIdString)
        } catch (e: IllegalArgumentException) {
            raise(CreateScopeError.ParentNotFound)
        }

        val exists = scopeRepository.existsById(parentId)
            .mapLeft { CreateScopeError.ExistenceCheckFailure(it) }
            .bind()

        if (!exists) {
            raise(CreateScopeError.ParentNotFound)
        }

        parentId
    }

    // ===== NEW SERVICE-SPECIFIC VALIDATION METHODS =====

    /**
     * Validates title format using service-specific errors.
     */
    private fun validateTitleWithServiceErrors(title: String): Either<CreateScopeError, Unit> =
        applicationScopeValidationService.validateTitleFormat(title)
            .mapLeft { titleError -> CreateScopeError.TitleValidationFailed(titleError) }

    /**
     * Validates hierarchy constraints using service-specific errors.
     */
    private suspend fun validateHierarchyWithServiceErrors(parentId: ScopeId?): Either<CreateScopeError, Unit> =
        applicationScopeValidationService.validateHierarchyConstraints(parentId)
            .mapLeft { businessRuleError -> CreateScopeError.BusinessRuleViolationFailed(businessRuleError) }

    /**
     * Validates title uniqueness using service-specific errors.
     */
    private suspend fun validateUniquenessWithServiceErrors(
        title: String, 
        parentId: ScopeId?
    ): Either<CreateScopeError, Unit> =
        applicationScopeValidationService.validateTitleUniquenessTyped(title, parentId)
            .mapLeft { uniquenessError -> CreateScopeError.DuplicateTitleFailed(uniquenessError) }

    /**
     * Validates repository-dependent business rules (LEGACY METHOD - KEPT FOR BACKWARDS COMPATIBILITY).
     * These cannot be checked in Scope.create as they require repository access.
     */
    private suspend fun validateScopeCreation(
        title: String, 
        description: String?, 
        parentId: ScopeId?
    ): Either<CreateScopeError, Unit> = either {
        val validationResult = applicationScopeValidationService.validateScopeCreation(
            title,
            description,
            parentId
        )
        
        when (validationResult) {
            is ValidationResult.Success -> Unit
            is ValidationResult.Failure -> {
                val firstError = validationResult.errors.first()
                raise(CreateScopeError.ValidationFailed("validation", firstError.toString()))
            }
        }
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
    ): Either<CreateScopeError, Scope> =
        Scope.create(
            title = title,
            description = description,
            parentId = parentId,
            metadata = metadata
        ).mapLeft { CreateScopeError.DomainRuleViolation(it) }

    /**
     * Persists the entity through repository.
     */
    private suspend fun saveScopeEntity(scope: Scope): Either<CreateScopeError, Scope> =
        scopeRepository.save(scope)
            .mapLeft { CreateScopeError.SaveFailure(it) }

}
