package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseQueryHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryEvaluator
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for filtering scopes using advanced aspect queries.
 * Supports comparison operators (=, !=, >, >=, <, <=) and logical operators (AND, OR, NOT).
 * Uses BaseQueryHandler for common functionality and centralized error mapping.
 */
class FilterScopesWithQueryHandler(
    private val scopeRepository: ScopeRepository,
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    transactionManager: TransactionManager,
    logger: Logger,
    private val parser: AspectQueryParser = AspectQueryParser(),
) : BaseQueryHandler<FilterScopesWithQuery, List<ScopeDto>>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    companion object {
        private const val SCOPE_REPOSITORY_SERVICE = "scope-repository"
    }

    override suspend fun executeQuery(query: FilterScopesWithQuery): Either<ScopeManagementApplicationError, List<ScopeDto>> = either {
                // Parse the query
                val ast = parser.parse(query.query).fold(
                    { _ ->
                        raise(
                            ScopesError.InvalidOperation(
                                operation = "filter-scopes-with-query",
                                reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_INPUT,
                            ).toGenericApplicationError(),
                        )
                    },
                    { it },
                )

                // Get all aspect definitions for type-aware comparison
                val definitions = aspectDefinitionRepository.findAll()
                    .mapLeft { error -> errorMappingService.mapRepositoryError(error, "filter-scopes-definitions") }
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
                            .mapLeft { error -> errorMappingService.mapRepositoryError(error, "filter-scopes-by-parent") }
                            .bind()
                    }
                    query.limit != 100 || query.offset > 0 -> {
                        // Use pagination - get all scopes with offset and limit
                        scopeRepository.findAll(query.offset, query.limit)
                            .mapLeft { error -> errorMappingService.mapRepositoryError(error, "filter-scopes-findall") }
                            .bind()
                    }
                    else -> {
                        // Default behavior - get root scopes only
                        scopeRepository.findAllRoot()
                            .mapLeft { error -> errorMappingService.mapRepositoryError(error, "filter-scopes-findroot") }
                            .bind()
                    }
                }

                // Filter scopes based on the query
                val filteredScopes = scopesToFilter.filter { scope ->
                    evaluator.evaluate(ast, scope.aspects)
                }

                // Map to DTOs
                filteredScopes.map { scope ->
                    ScopeMapper.toDto(scope)
                }
            }
}
