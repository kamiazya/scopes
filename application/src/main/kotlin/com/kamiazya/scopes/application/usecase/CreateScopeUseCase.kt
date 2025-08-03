package com.kamiazya.scopes.application.usecase

import com.kamiazya.scopes.application.error.ApplicationError
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.flatMap
import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.repository.ScopeRepository
import com.kamiazya.scopes.domain.service.ScopeValidationService

/**
 * Use case for creating new Scope entities.
 * Follows functional DDD principles with explicit error handling and pure functions.
 */
class CreateScopeUseCase(
    private val scopeRepository: ScopeRepository,
) {

    suspend fun execute(request: CreateScopeRequest): Either<ApplicationError, CreateScopeResponse> = either {
        val validRequest = validateRequest(request).bind()
        checkParentExists(validRequest.parentId).bind()
        validateBusinessRules(request).bind()
        val scope = createScopeEntity(validRequest).bind()
        val savedScope = saveScopeEntity(scope).bind()
        CreateScopeResponse(savedScope)
    }

    private fun validateRequest(request: CreateScopeRequest): Either<ApplicationError, CreateScopeRequest> = either {
        ScopeValidationService.validateTitle(request.title)
            .mapLeft { ApplicationError.fromDomainError(it) }
            .bind()
        ScopeValidationService.validateDescription(request.description)
            .mapLeft { ApplicationError.fromDomainError(it) }
            .bind()
        request
    }

    private suspend fun checkParentExists(parentId: ScopeId?): Either<ApplicationError, Unit> = either {
        if (parentId == null) return@either

        val exists = scopeRepository.existsById(parentId)
            .mapLeft { ApplicationError.fromRepositoryError(it) }
            .bind()

        if (!exists) {
            raise(
                ApplicationError.fromDomainError(
                    com.kamiazya.scopes.domain.error.DomainError.ScopeError.ScopeNotFound
                )
            )
        }
    }

    private suspend fun validateBusinessRules(
        request: CreateScopeRequest
    ): Either<ApplicationError, CreateScopeRequest> = either {
        val allScopes = scopeRepository.findAll()
            .mapLeft { ApplicationError.fromRepositoryError(it) }
            .bind()

        // Validate hierarchy depth
        ScopeValidationService.validateHierarchyDepth(request.parentId, allScopes)
            .mapLeft { ApplicationError.fromDomainError(it) }
            .bind()

        // Validate children limit
        ScopeValidationService.validateChildrenLimit(request.parentId, allScopes)
            .mapLeft { ApplicationError.fromDomainError(it) }
            .bind()

        // Validate title uniqueness
        ScopeValidationService.validateTitleUniqueness(
            request.title,
            request.parentId,
            null,
            allScopes
        ).mapLeft { ApplicationError.fromDomainError(it) }
            .bind()

        request
    }

    private fun createScopeEntity(request: CreateScopeRequest): Either<ApplicationError, Scope> = either {
        Scope.create(
            id = request.id ?: ScopeId.generate(),
            title = request.title,
            description = request.description,
            parentId = request.parentId,
            metadata = request.metadata
        )
    }

    private suspend fun saveScopeEntity(scope: Scope): Either<ApplicationError, Scope> =
        scopeRepository.save(scope)
            .mapLeft { ApplicationError.fromRepositoryError(it) }
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

