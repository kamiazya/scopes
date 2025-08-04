package io.github.kamiazya.scopes.application.usecase

import io.github.kamiazya.scopes.application.error.ApplicationError
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.flatMap
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.ScopeValidationService

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
            .mapLeft { ApplicationError.Domain(it) }
            .bind()
        ScopeValidationService.validateDescription(request.description)
            .mapLeft { ApplicationError.Domain(it) }
            .bind()
        request
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

    private suspend fun validateBusinessRules(
        request: CreateScopeRequest
    ): Either<ApplicationError, CreateScopeRequest> = either {
        // Validate hierarchy depth using efficient query
        ScopeValidationService.validateHierarchyDepthEfficient(request.parentId, scopeRepository)
            .mapLeft { ApplicationError.Domain(it) }
            .bind()

        // Validate children limit using efficient query
        ScopeValidationService.validateChildrenLimitEfficient(request.parentId, scopeRepository)
            .mapLeft { ApplicationError.Domain(it) }
            .bind()

        // Validate title uniqueness using efficient query
        ScopeValidationService.validateTitleUniquenessEfficient(
            request.title,
            request.parentId,
            scopeRepository
        ).mapLeft { ApplicationError.Domain(it) }
            .bind()

        request
    }

    private fun createScopeEntity(request: CreateScopeRequest): Either<ApplicationError, Scope> = either {
        val scope = if (request.id != null) {
            // For cases where ID is specified, create directly using data class constructor
            // since validation was already done in validateBusinessRules
            Scope(
                id = request.id,
                title = request.title,
                description = request.description,
                parentId = request.parentId,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now(),
                metadata = request.metadata
            )
        } else {
            // Use the public factory method with validation
            Scope.create(
                title = request.title,
                description = request.description,
                parentId = request.parentId,
                metadata = request.metadata
            ).mapLeft { ApplicationError.Domain(it) }
                .bind()
        }
        scope
    }

    private suspend fun saveScopeEntity(scope: Scope): Either<ApplicationError, Scope> =
        scopeRepository.save(scope)
            .mapLeft { ApplicationError.Repository(it) }
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

