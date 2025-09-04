package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for GetScopeById query.
 * Retrieves a scope by its ID and returns it as a DTO.
 */
class GetScopeByIdHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager) :
    QueryHandler<GetScopeById, ScopesError, ScopeDto?> {

    override suspend operator fun invoke(query: GetScopeById): Either<ScopesError, ScopeDto?> = transactionManager.inTransaction {
        either {
            // Parse and validate the scope ID
            val scopeId = ScopeId.create(query.id).bind()

            // Retrieve the scope from repository
            val scope = scopeRepository.findById(scopeId)
                .mapLeft { ScopesError.SystemError("Failed to find scope: $it") }
                .bind()

            // Map to DTO if found
            scope?.let { ScopeMapper.toDto(it) }
        }
    }
}
