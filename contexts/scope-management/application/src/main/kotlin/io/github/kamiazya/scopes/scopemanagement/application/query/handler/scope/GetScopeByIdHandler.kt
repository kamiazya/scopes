package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for GetScopeById query.
 * Retrieves a scope by its ID and returns it as a DTO.
 */
class GetScopeByIdHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    QueryHandler<GetScopeById, ScopeManagementApplicationError, ScopeDto?> {

    override suspend operator fun invoke(query: GetScopeById): Either<ScopeManagementApplicationError, ScopeDto?> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting scope by ID",
            mapOf(
                "scopeId" to query.id,
            ),
        )

        either {
            // Parse and validate the scope ID
            val scopeId = ScopeId.create(query.id)
                .mapLeft { it.toGenericApplicationError() }
                .bind()

            // Retrieve the scope from repository
            val scope = scopeRepository.findById(scopeId)
                .mapLeft { _ ->
                    ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                        operation = "findById",
                    )
                }
                .bind()

            // Map to DTO if found
            val result = scope?.let { ScopeMapper.toDto(it) }

            logger.info(
                "Scope lookup completed",
                mapOf(
                    "scopeId" to scopeId.value.toString(),
                    "found" to (result != null).toString(),
                ),
            )

            result
        }
    }.onLeft { error ->
        logger.error(
            "Failed to get scope by ID",
            mapOf(
                "scopeId" to query.id,
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
