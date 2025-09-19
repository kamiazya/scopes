package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseQueryHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for GetScopeById query.
 * Uses BaseQueryHandler for common functionality and centralized error mapping.
 * Retrieves a scope by its ID and returns it as a DTO.
 */
class GetScopeByIdHandler(private val scopeRepository: ScopeRepository, transactionManager: TransactionManager, logger: Logger) :
    BaseQueryHandler<GetScopeById, ScopeDto?>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeQuery(query: GetScopeById): Either<ScopeManagementApplicationError, ScopeDto?> = either {
        // Parse and validate the scope ID
        val scopeId = ScopeId.create(query.id)
            .mapLeft { errorMappingService.mapDomainError(it, "get-scope-id") }
            .bind()

        // Retrieve the scope from repository
        val scope = scopeRepository.findById(scopeId)
            .mapLeft { error -> errorMappingService.mapRepositoryError(error, "find-scope-by-id") }
            .bind()

        // Map to DTO if found
        scope?.let { ScopeMapper.toDto(it) }
    }
}
