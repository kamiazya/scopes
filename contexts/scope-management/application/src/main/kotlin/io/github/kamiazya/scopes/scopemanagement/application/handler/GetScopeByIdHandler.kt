package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for GetScopeById query.
 * Retrieves a scope by its ID and returns it as a DTO.
 */
class GetScopeByIdHandler(private val scopeRepository: ScopeRepository, private val logger: Logger) : UseCase<GetScopeById, ScopesError, ScopeDto> {

    override suspend operator fun invoke(input: GetScopeById): Either<ScopesError, ScopeDto> = either {
        logger.debug("Getting scope by ID", mapOf("scopeId" to input.id))

        // Parse and validate the scope ID
        val scopeId = ScopeId.create(input.id).mapLeft { idError ->
            logger.warn("Invalid scope ID format", mapOf("scopeId" to input.id))
            ScopeInputError.IdError.InvalidFormat(
                attemptedValue = input.id,
                occurredAt = Clock.System.now(),
            )
        }.bind()

        // Retrieve the scope from repository
        val scope = scopeRepository.findById(scopeId).bind()

        if (scope == null) {
            logger.warn("Scope not found", mapOf("scopeId" to scopeId.value))
            raise(
                ScopeNotFoundError(
                    scopeId = scopeId,
                    occurredAt = Clock.System.now(),
                ),
            )
        }

        logger.debug(
            "Scope retrieved successfully",
            mapOf(
                "scopeId" to scope.id.value,
                "title" to scope.title.value,
            ),
        )

        // Map to DTO
        ScopeMapper.toDto(scope)
    }.onLeft { error ->
        logger.error(
            "Failed to get scope by ID",
            mapOf(
                "scopeId" to input.id,
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
