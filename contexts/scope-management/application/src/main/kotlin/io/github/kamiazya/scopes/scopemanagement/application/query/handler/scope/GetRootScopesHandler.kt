package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository

/**
 * Handler for getting root scopes (scopes without parent).
 */
class GetRootScopesHandler(
    private val scopeRepository: ScopeRepository,
    private val aliasRepository: ScopeAliasRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : QueryHandler<GetRootScopes, ScopeContractError, ScopeListResult> {

    override suspend operator fun invoke(query: GetRootScopes): Either<ScopeContractError, ScopeListResult> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting root scopes",
            mapOf(
                "offset" to query.offset,
                "limit" to query.limit,
            ),
        )
        either {
            // Get root scopes (parentId = null) with database-side pagination
            val rootScopes = scopeRepository.findByParentId(null, query.offset, query.limit)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()
            val totalCount = scopeRepository.countByParentId(null)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()

            // Get all canonical aliases for the root scopes in batch
            val scopeIds = rootScopes.map { it.id }
            val canonicalAliasesMap = if (scopeIds.isNotEmpty()) {
                aliasRepository.findCanonicalByScopeIds(scopeIds)
                    .mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error)
                    }
                    .bind()
                    .associateBy { it.scopeId }
            } else {
                emptyMap()
            }

            // Map each root scope to ScopeResult with canonical alias
            val scopeResults = rootScopes.map { scope ->
                // Get canonical alias from batch result
                val canonicalAlias = canonicalAliasesMap[scope.id]

                // Missing canonical alias is a data consistency error
                if (canonicalAlias == null) {
                    raise(
                        ScopeContractError.DataInconsistency.MissingCanonicalAlias(
                            scopeId = scope.id.toString(),
                        ),
                    )
                }

                ScopeResult(
                    id = scope.id.toString(),
                    title = scope.title.value,
                    description = scope.description?.value,
                    parentId = scope.parentId?.toString(),
                    canonicalAlias = canonicalAlias.aliasName.value,
                    createdAt = scope.createdAt,
                    updatedAt = scope.updatedAt,
                    isArchived = (scope.status is io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeStatus.Archived),
                    aspects = scope.aspects.toMap().mapKeys { (key, _) ->
                        key.value
                    }.mapValues { (_, values) ->
                        values.map { it.value }
                    },
                )
            }

            val result = ScopeListResult(
                scopes = scopeResults,
                offset = query.offset,
                limit = query.limit,
                totalCount = totalCount,
            )

            logger.info(
                "Successfully retrieved root scopes",
                mapOf(
                    "count" to result.scopes.size,
                    "totalCount" to totalCount,
                    "offset" to query.offset,
                    "limit" to query.limit,
                ),
            )

            result
        }
    }.onLeft { error ->
        logger.error(
            "Failed to get root scopes",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
