package io.github.kamiazya.scopes.application.usecase

import io.github.kamiazya.scopes.application.error.ApplicationError
import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.ScopeValidationService
import io.github.kamiazya.scopes.domain.error.firstErrorOnly

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

        // Perform repository-dependent validations (hierarchy depth, children limit, title uniqueness)
        // These validations require repository access and cannot be done in Scope.create
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
                ApplicationError.DomainErrors.single(
                    io.github.kamiazya.scopes.domain.error.DomainError.ScopeError.ScopeNotFound
                )
            )
        }
    }

    private fun createScopeEntity(request: CreateScopeRequest): Either<ApplicationError, Scope> = either {
        // Use the public factory method which includes validation
        val scope = Scope.create(
            title = request.title,
            description = request.description,
            parentId = request.parentId,
            metadata = request.metadata
        ).mapLeft { ApplicationError.DomainErrors.single(it) }
            .bind()
        scope
    }

    private suspend fun saveScopeEntity(scope: Scope): Either<ApplicationError, Scope> =
        scopeRepository.save(scope)
            .mapLeft { ApplicationError.Repository(it) }

    /**
     * Validates repository-dependent business rules that cannot be checked in Scope.create.
     * These include hierarchy depth, children limit, and title uniqueness within parent scope.
     */
    private suspend fun validateScopeCreationConsolidated(
        request: CreateScopeRequest
    ): Either<ApplicationError, Unit> {
        // Use ScopeValidationService for repository-dependent validations
        val validationResult = ScopeValidationService.validateScopeCreation(
            request.title,
            request.description,
            request.parentId,
            scopeRepository
        )

        // Always accumulate errors for better UX
        // Callers can use firstErrorOnly() if they need fail-fast behavior
        return ApplicationError.fromValidationResult(validationResult).map { }
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
)

/**
 * Response DTO for scope creation.
 */
data class CreateScopeResponse(
    val scope: Scope,
)

