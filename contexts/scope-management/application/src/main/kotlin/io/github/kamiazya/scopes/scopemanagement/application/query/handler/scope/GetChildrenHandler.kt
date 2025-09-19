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
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetChildren
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for getting children of a scope.
 */
class GetChildrenHandler(
    private val scopeRepository: ScopeRepository,
    private val aliasRepository: ScopeAliasRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : QueryHandler<GetChildren, ScopeContractError, ScopeListResult> {

    override suspend operator fun invoke(input: GetChildren): Either<ScopeContractError, ScopeListResult> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting children of scope",
            mapOf(
                "parentId" to (input.parentId ?: "null"),
                "offset" to input.offset,
                "limit" to input.limit,
            ),
        )

        either {
            // Parse parent ID if provided
            val parentId = input.parentId?.let { parentIdString ->
                ScopeId.create(parentIdString)
                    .mapLeft { error ->
                        applicationErrorMapper.mapDomainError(
                            error,
                            ErrorMappingContext(attemptedValue = parentIdString),
                        )
                    }
                    .bind()
            }

            // Get children from repository with database-side pagination
            val children = scopeRepository.findByParentId(parentId, input.offset, input.limit)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()
            val totalCount = scopeRepository.countByParentId(parentId)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()

            // Get all canonical aliases for the children in batch
            val scopeIds = children.map { it.id }
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

            val scopeResults = children.map { scope ->
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

                // Map to Contract DTO
                ScopeResult(
                    id = scope.id.toString(),
                    title = scope.title.value,
                    description = scope.description?.value,
                    parentId = scope.parentId?.toString(),
                    canonicalAlias = canonicalAlias.aliasName.value,
                    createdAt = scope.createdAt,
                    updatedAt = scope.updatedAt,
                    isArchived = false,
                    aspects = scope.aspects.toMap().mapKeys { (key, _) ->
                        key.value
                    }.mapValues { (_, values) ->
                        values.map { it.value }
                    },
                )
            }

            val result = ScopeListResult(
                scopes = scopeResults,
                offset = input.offset,
                limit = input.limit,
                totalCount = totalCount,
            )

            logger.info(
                "Successfully retrieved children of scope",
                mapOf(
                    "parentId" to (parentId?.value?.toString() ?: "null"),
                    "count" to result.scopes.size,
                    "totalCount" to totalCount,
                    "offset" to input.offset,
                    "limit" to input.limit,
                ),
            )

            result
        }
    }.onLeft { error ->
        logger.error(
            "Failed to get children of scope",
            mapOf(
                "parentId" to (input.parentId ?: "null"),
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
