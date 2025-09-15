package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.domain.error.QueryParseError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
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
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
    private val parser: AspectQueryParser = AspectQueryParser(),
) : QueryHandler<FilterScopesWithQuery, ScopeManagementApplicationError, List<ScopeDto>> {

    override suspend operator fun invoke(query: FilterScopesWithQuery): Either<ScopeManagementApplicationError, List<ScopeDto>> =
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
                    { error ->
                        raise(
                            ContextError.InvalidFilter(
                                filter = query.query,
                                reason = formatParseError(error),
                            ),
                        )
                    },
                    { it },
                )

                // Get all aspect definitions for type-aware comparison
                val definitions = aspectDefinitionRepository.findAll()
                    .mapLeft { error ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "findAll",
                            errorCause = error.toString(),
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
                            .mapLeft { it.toGenericApplicationError() }
                            .bind()
                        scopeRepository.findByParentId(parentScopeId, offset = 0, limit = 1000)
                            .mapLeft { error ->
                                ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                                    operation = "findByParentId",
                                    errorCause = error.toString(),
                                )
                            }
                            .bind()
                    }
                    query.limit != 100 || query.offset > 0 -> {
                        // Use pagination - get all scopes with offset and limit
                        scopeRepository.findAll(query.offset, query.limit)
                            .mapLeft { error ->
                                ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                                    operation = "findAll",
                                    errorCause = error.toString(),
                                )
                            }
                            .bind()
                    }
                    else -> {
                        // Default behavior - get root scopes only
                        scopeRepository.findAllRoot()
                            .mapLeft { error ->
                                ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                                    operation = "findAllRoot",
                                    errorCause = error.toString(),
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

    private fun formatParseError(error: QueryParseError): String = when (error) {
        is QueryParseError.EmptyQuery ->
            "Query cannot be empty"
        is QueryParseError.UnexpectedCharacter ->
            "Unexpected character '${error.char}' at position ${error.position}"
        is QueryParseError.UnterminatedString ->
            "Unterminated string at position ${error.position}"
        is QueryParseError.UnexpectedToken ->
            "Unexpected token at position ${error.position}"
        is QueryParseError.MissingClosingParen ->
            "Missing closing parenthesis at position ${error.position}"
        is QueryParseError.ExpectedExpression ->
            "Expected expression at position ${error.position}"
        is QueryParseError.ExpectedIdentifier ->
            "Expected identifier at position ${error.position}"
        is QueryParseError.ExpectedOperator ->
            "Expected operator at position ${error.position}"
        is QueryParseError.ExpectedValue ->
            "Expected value at position ${error.position}"
    }
}
