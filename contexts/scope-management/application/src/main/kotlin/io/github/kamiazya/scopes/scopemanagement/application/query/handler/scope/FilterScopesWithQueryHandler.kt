package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryEvaluator
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for filtering scopes using advanced aspect queries.
 * Supports comparison operators (=, !=, >, >=, <, <=) and logical operators (AND, OR, NOT).
 */
class FilterScopesWithQueryHandler(
    private val scopeRepository: ScopeRepository,
    private val aliasRepository: ScopeAliasRepository,
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
    private val parser: AspectQueryParser = AspectQueryParser(),
) : QueryHandler<FilterScopesWithQuery, ScopeContractError, List<ScopeResult>> {

    companion object {
        private const val SCOPE_REPOSITORY_SERVICE = "scope-repository"
    }

    override suspend operator fun invoke(query: FilterScopesWithQuery): Either<ScopeContractError, List<ScopeResult>> =
        transactionManager.inReadOnlyTransaction {
            logger.debug(
                "Filtering scopes with query",
                mapOf(
                    "query" to query.query,
                    "parentId" to (query.parentId ?: "none"),
                    "offset" to query.offset,
                    "limit" to query.limit,
                ),
            )
            either {
                // Parse the query
                val ast = parser.parse(query.query).fold(
                    { _ ->
                        raise(
                            applicationErrorMapper.mapDomainError(
                                ScopesError.InvalidOperation(
                                    operation = "filter-scopes-with-query",
                                    reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_INPUT,
                                ),
                            ),
                        )
                    },
                    { it },
                )

                // Get all aspect definitions for type-aware comparison
                val definitions = aspectDefinitionRepository.findAll()
                    .mapLeft { _ ->
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "Aspect definition service",
                        )
                    }
                    .bind()
                    .associateBy { it.key.value }

                // Create evaluator with definitions
                val evaluator = AspectQueryEvaluator(definitions)

                // Get scopes to filter
                val scopesToFilter = when {
                    query.parentId != null -> {
                        val parentScopeId = ScopeId.create(query.parentId)
                            .mapLeft { error ->
                                applicationErrorMapper.mapDomainError(
                                    error,
                                    ErrorMappingContext(attemptedValue = query.parentId),
                                )
                            }
                            .bind()
                        scopeRepository.findByParentId(parentScopeId, offset = 0, limit = 1000)
                            .mapLeft { error ->
                                applicationErrorMapper.mapDomainError(error)
                            }
                            .bind()
                    }
                    query.limit != 100 || query.offset > 0 -> {
                        // Use pagination - get all scopes with offset and limit
                        scopeRepository.findAll(query.offset, query.limit)
                            .mapLeft { error ->
                                applicationErrorMapper.mapDomainError(error)
                            }
                            .bind()
                    }
                    else -> {
                        // Default behavior - get root scopes only
                        scopeRepository.findAllRoot()
                            .mapLeft { error ->
                                applicationErrorMapper.mapDomainError(error)
                            }
                            .bind()
                    }
                }

                // Filter scopes based on the query
                val filteredScopes = scopesToFilter.filter { scope ->
                    evaluator.evaluate(ast, scope.aspects)
                }

                // Get all canonical aliases for the filtered scopes in batch
                val filteredScopeIds = filteredScopes.map { it.id }
                val canonicalAliasesMap = if (filteredScopeIds.isNotEmpty()) {
                    aliasRepository.findCanonicalByScopeIds(filteredScopeIds)
                        .mapLeft { error ->
                            applicationErrorMapper.mapDomainError(error)
                        }
                        .bind()
                        .associateBy { it.scopeId }
                } else {
                    emptyMap()
                }

                // Map to Contract DTOs
                val result = filteredScopes.map { scope ->
                    // Get canonical alias from batch result
                    val canonicalAlias = canonicalAliasesMap[scope.id]

                    // Missing canonical alias is a data consistency error
                    if (canonicalAlias == null) {
                        raise(ScopeContractError.DataInconsistency.MissingCanonicalAlias(
                            scopeId = scope.id.toString()
                        ))
                    }

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

                logger.info(
                    "Successfully filtered scopes with query",
                    mapOf(
                        "query" to query.query,
                        "parentId" to (query.parentId ?: "none"),
                        "totalScopes" to scopesToFilter.size,
                        "filteredScopes" to result.size,
                    ),
                )

                result
            }
        }.onLeft { error ->
            logger.error(
                "Failed to filter scopes with query",
                mapOf(
                    "query" to query.query,
                    "parentId" to (query.parentId ?: "none"),
                    "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                    "message" to error.toString(),
                ),
            )
        }
}
