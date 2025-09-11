package io.github.kamiazya.scopes.scopemanagement.application.handler.query.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.alias.AliasDto
import io.github.kamiazya.scopes.scopemanagement.application.query.scope.ListAliases
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for listing all aliases for a specific scope.
 * Returns aliases sorted with canonical first, then by creation date.
 */
class ListAliasesHandler(
    private val scopeAliasRepository: ScopeAliasRepository,
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : QueryHandler<ListAliases, ScopesError, List<AliasDto>> {

    override suspend operator fun invoke(query: ListAliases): Either<ScopesError, List<AliasDto>> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Listing aliases for scope",
            mapOf(
                "scopeId" to query.scopeId,
            ),
        )
        either {
            // Validate and create scope ID
            val scopeId = ScopeId.create(query.scopeId).bind()

            // Get the scope to verify it exists
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
                ?: raise(
                    ScopesError.NotFound(
                        entityType = "Scope",
                        identifier = query.scopeId,
                        identifierType = "id",
                        occurredAt = Clock.System.now(),
                    ),
                )

            // Get all aliases for the scope
            val aliases = scopeAliasRepository.findByScopeId(scopeId)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "alias-repository",
                        cause = error as? Throwable,
                        context = mapOf(
                            "operation" to "findByScopeId",
                            "scopeId" to scopeId.value.toString(),
                        ),
                        occurredAt = Clock.System.now(),
                    )
                }
                .bind()

            // Map to DTOs
            val result = aliases.map { alias ->
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

            logger.info(
                "Successfully listed aliases for scope",
                mapOf(
                    "scopeId" to scopeId.value.toString(),
                    "aliasCount" to result.size,
                ),
            )

            result
        }
    }.onLeft { error ->
        logger.error(
            "Failed to list aliases",
            mapOf(
                "scopeId" to query.scopeId,
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
