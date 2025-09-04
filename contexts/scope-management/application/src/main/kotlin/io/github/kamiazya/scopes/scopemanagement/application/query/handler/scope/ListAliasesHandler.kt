package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.alias.AliasDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListAliases
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for listing all aliases for a specific scope.
 * Returns aliases sorted with canonical first, then by creation date.
 */
class ListAliasesHandler(
    private val scopeAliasRepository: ScopeAliasRepository,
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
) : QueryHandler<ListAliases, ScopesError, List<AliasDto>> {

    override suspend operator fun invoke(query: ListAliases): Either<ScopesError, List<AliasDto>> = transactionManager.inTransaction {
        either {
            // Validate and create scope ID
            val scopeId = ScopeId.create(query.scopeId).bind()

            // Get the scope to verify it exists
            val scope = scopeRepository.findById(scopeId)
                .mapLeft { ScopesError.SystemError("Failed to find scope: $it") }
                .bind()
                ?: raise(ScopesError.NotFound("Scope not found: ${query.scopeId}"))

            // Get all aliases for the scope
            val aliases = scopeAliasRepository.findByScopeId(scopeId)
                .mapLeft { ScopesError.SystemError("Failed to find aliases: $it") }
                .bind()

            // Map to DTOs
            aliases.map { alias ->
                AliasDto(
                    alias = alias.aliasName.value,
                    scopeId = alias.scopeId.value.toString(),
                    isCanonical = alias.isCanonical(),
                    createdAt = alias.createdAt,
                )
            }.sortedWith(
                compareByDescending<AliasDto> { it.isCanonical }
                    .thenBy { it.alias },
            )
        }
    }
}
