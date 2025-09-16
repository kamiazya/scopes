package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryEvaluator
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for filtering scopes using advanced aspect queries.
 * Supports comparison operators (=, !=, >, >=, <, <=) and logical operators (AND, OR, NOT).
 */
class FilterScopesWithQueryHandler(
    private val scopeRepository: ScopeRepository,
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
    private val parser: AspectQueryParser = AspectQueryParser(),
) : QueryHandler<FilterScopesWithQuery, ScopesError, List<ScopeDto>> {

    companion object {
        private const val SCOPE_REPOSITORY_SERVICE = "scope-repository"
    }

    override suspend operator fun invoke(query: FilterScopesWithQuery): Either<ScopesError, List<ScopeDto>> = transactionManager.inReadOnlyTransaction {
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
                        ScopesError.InvalidOperation(
                            operation = "filter-scopes-with-query",
                            reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_INPUT,
                            occurredAt = Clock.System.now(),
                        ),
                    )
                },
                { it },
            )

            // Get all aspect definitions for type-aware comparison
            val definitions = aspectDefinitionRepository.findAll()
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "aspect-repository",
                        cause = error as? Throwable,
                        context = mapOf("operation" to "findAll"),
                        occurredAt = Clock.System.now(),
                    )
                }
                .bind()
                .associateBy { it.key.value }

            // Create evaluator with definitions
            val evaluator = AspectQueryEvaluator(definitions)

            // Get scopes to filter
            val scopesToFilter = when {
                query.parentId != null -> {
                    val parentScopeId = ScopeId.create(query.parentId).bind()
                    scopeRepository.findByParentId(parentScopeId, offset = 0, limit = 1000)
                        .mapLeft { error ->
                            ScopesError.SystemError(
                                errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                                service = SCOPE_REPOSITORY_SERVICE,
                                cause = error as? Throwable,
                                context = mapOf(
                                    "operation" to "findByParentId",
                                    "parentId" to parentScopeId.value.toString(),
                                ),
                                occurredAt = Clock.System.now(),
                            )
                        }
                        .bind()
                }
                query.limit != 100 || query.offset > 0 -> {
                    // Use pagination - get all scopes with offset and limit
                    scopeRepository.findAll(query.offset, query.limit)
                        .mapLeft { error ->
                            ScopesError.SystemError(
                                errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                                service = SCOPE_REPOSITORY_SERVICE,
                                cause = error as? Throwable,
                                context = mapOf(
                                    "operation" to "findAll",
                                    "offset" to query.offset,
                                    "limit" to query.limit,
                                ),
                                occurredAt = Clock.System.now(),
                            )
                        }
                        .bind()
                }
                else -> {
                    // Default behavior - get root scopes only
                    scopeRepository.findAllRoot()
                        .mapLeft { error ->
                            ScopesError.SystemError(
                                errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                                service = SCOPE_REPOSITORY_SERVICE,
                                cause = error as? Throwable,
                                context = mapOf("operation" to "findAllRoot"),
                                occurredAt = Clock.System.now(),
                            )
                        }
                        .bind()
                }
            }

            // Filter scopes based on the query
            val filteredScopes = scopesToFilter.filter { scope ->
                evaluator.evaluate(ast, scope.aspects)
            }

            // Map to DTOs
            val result = filteredScopes.map { scope ->
                ScopeMapper.toDto(scope)
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
