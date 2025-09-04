package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.domain.error.QueryParseError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
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
    private val parser: AspectQueryParser = AspectQueryParser(),
) : QueryHandler<FilterScopesWithQuery, ScopesError, List<ScopeDto>> {

    override suspend operator fun invoke(query: FilterScopesWithQuery): Either<ScopesError, List<ScopeDto>> = transactionManager.inTransaction {
        either {
            // Parse the query
            val ast = parser.parse(query.query).fold(
                { error ->
                    raise(ScopesError.InvalidOperation("Invalid query: ${formatParseError(error)}"))
                },
                { it },
            )

            // Get all aspect definitions for type-aware comparison
            val definitions = aspectDefinitionRepository.findAll()
                .mapLeft { ScopesError.SystemError("Failed to load aspect definitions: $it") }
                .bind()
                .associateBy { it.key.value }

            // Create evaluator with definitions
            val evaluator = AspectQueryEvaluator(definitions)

            // Get scopes to filter
            val scopesToFilter = when {
                query.parentId != null -> {
                    val parentScopeId = ScopeId.create(query.parentId).bind()
                    scopeRepository.findByParentId(parentScopeId, offset = 0, limit = 1000)
                        .mapLeft { ScopesError.SystemError("Failed to find scopes: $it") }
                        .bind()
                }
                query.offset > 0 || query.limit < 100 -> {
                    // Use pagination - get all scopes with offset and limit
                    scopeRepository.findAll(query.offset, query.limit)
                        .mapLeft { ScopesError.SystemError("Failed to find scopes: $it") }
                        .bind()
                }
                else -> {
                    // Default behavior - get root scopes only
                    scopeRepository.findAllRoot()
                        .mapLeft { ScopesError.SystemError("Failed to find root scopes: $it") }
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
