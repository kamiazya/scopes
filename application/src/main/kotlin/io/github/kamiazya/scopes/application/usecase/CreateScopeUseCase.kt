package io.github.kamiazya.scopes.application.usecase

import io.github.kamiazya.scopes.application.error.ApplicationError
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.flatMap
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.ScopeValidationService
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.validationSuccess
import io.github.kamiazya.scopes.domain.error.validationFailure
import io.github.kamiazya.scopes.domain.error.firstErrorOnly
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf

/**
 * Use case for creating new Scope entities.
 * Follows functional DDD principles with explicit error handling and pure functions.
 */
class CreateScopeUseCase(
    private val scopeRepository: ScopeRepository,
) {

    suspend fun execute(request: CreateScopeRequest): Either<ApplicationError, CreateScopeResponse> = either {
        // Check parent exists (this is a prerequisite for other validations)
        checkParentExists(request.parentId).bind()

        // Perform comprehensive validation using consolidated method
        validateScopeCreationConsolidated(request).bind()

        // Create and save the scope entity
        val scope = createScopeEntity(request).bind()
        val savedScope = saveScopeEntity(scope).bind()
        CreateScopeResponse(savedScope)
    }

    private suspend fun checkParentExists(parentId: ScopeId?): Either<ApplicationError, Unit> = either {
        if (parentId == null) return@either

        val exists = scopeRepository.existsById(parentId)
            .mapLeft { ApplicationError.Repository(it) }
            .bind()

        if (!exists) {
            raise(
                ApplicationError.Domain(
                    io.github.kamiazya.scopes.domain.error.DomainError.ScopeError.ScopeNotFound
                )
            )
        }
    }

    private fun createScopeEntity(request: CreateScopeRequest): Either<ApplicationError, Scope> = either {
        // Always use the public factory method for consistency
        // Validation is already done in validateScopeCreationConsolidated
        val scope = Scope.create(
            title = request.title,
            description = request.description,
            parentId = request.parentId,
            metadata = request.metadata
        ).mapLeft { ApplicationError.Domain(it) }
            .bind()
        scope
    }

    private suspend fun saveScopeEntity(scope: Scope): Either<ApplicationError, Scope> =
        scopeRepository.save(scope)
            .mapLeft { ApplicationError.Repository(it) }

    /**
     * Consolidated validation using single accumulating method with mode flexibility.
     * Uses ValidationResult for error accumulation and applies mode-specific processing.
     */
    private suspend fun validateScopeCreationConsolidated(
        request: CreateScopeRequest
    ): Either<ApplicationError, Unit> {
        // Always use accumulating validation for consistency
        val validationResult = ScopeValidationService.validateScopeCreation(
            request.title,
            request.description,
            request.parentId,
            scopeRepository
        )

        // Apply fail-fast behavior if requested
        val finalResult = when (request.validationMode) {
            ScopeValidationService.ValidationMode.FAIL_FAST -> validationResult.firstErrorOnly()
            ScopeValidationService.ValidationMode.ACCUMULATE -> validationResult
        }

        return ApplicationError.fromValidationResult(finalResult)
    }
}

/**
 * Request DTO for scope creation.
 */
data class CreateScopeRequest(
    val id: ScopeId? = null,
    val title: String,
    val description: String? = null,
    val parentId: ScopeId? = null,
    val metadata: Map<String, String> = emptyMap(),
    val validationMode: ScopeValidationService.ValidationMode = ScopeValidationService.ValidationMode.ACCUMULATE,
)

/**
 * Response DTO for scope creation.
 */
data class CreateScopeResponse(
    val scope: Scope,
)

