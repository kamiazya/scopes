package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
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
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : QueryHandler<ListAliases, ScopeContractError, AliasListResult> {

    override suspend operator fun invoke(query: ListAliases): Either<ScopeContractError, AliasListResult> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Listing aliases for scope",
            mapOf(
                "scopeId" to query.scopeId,
            ),
        )
        either {
            // Validate and create scope ID
            val scopeId = ScopeId.create(query.scopeId)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(
                        error,
                        ErrorMappingContext(attemptedValue = query.scopeId),
                    )
                }
                .bind()

            // Get the scope to verify it exists
            scopeRepository.findById(scopeId)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()
                ?: raise(
                    applicationErrorMapper.mapDomainError(
                        ScopesError.NotFound(
                            entityType = "Scope",
                            identifier = query.scopeId,
                            identifierType = "id",
                        ),
                    ),
                )

            // Get all aliases for the scope
            val aliases = scopeAliasRepository.findByScopeId(scopeId)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()

            // Map to Contract DTOs
            val aliasInfos = aliases.map { alias ->
                AliasInfo(
                    aliasName = alias.aliasName.value,
                    aliasType = if (alias.isCanonical()) "canonical" else "regular",
                    isCanonical = alias.isCanonical(),
                    createdAt = alias.createdAt,
                )
            }.sortedWith(
                compareByDescending<AliasInfo> { it.isCanonical }
                    .thenBy { it.aliasName },
            )

            val result = AliasListResult(
                scopeId = query.scopeId,
                aliases = aliasInfos,
                totalCount = aliasInfos.size,
            )

            logger.info(
                "Successfully listed aliases for scope",
                mapOf(
                    "scopeId" to scopeId.value.toString(),
                    "aliasCount" to result.totalCount,
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
