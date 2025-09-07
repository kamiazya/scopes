package io.github.kamiazya.scopes.scopemanagement.application.handler.query.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for GetScopeById query.
 * Retrieves a scope by its ID and returns it as a DTO.
 */
class GetScopeByIdHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    QueryHandler<GetScopeById, ScopesError, ScopeDto?> {

    override suspend operator fun invoke(query: GetScopeById): Either<ScopesError, ScopeDto?> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting scope by ID",
            mapOf(
                "scopeId" to query.id,
            ),
        )

        either {
            // Parse and validate the scope ID
            val scopeId = ScopeId.create(query.id).bind()

            // Retrieve the scope from repository
            val scope = scopeRepository.findById(scopeId)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "scope-repository",
                        cause = error as? Throwable,
                        context = mapOf(
                            "operation" to "findById",
                            "scopeId" to scopeId.value.toString(),
                        ),
                        occurredAt = Clock.System.now(),
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
