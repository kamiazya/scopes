package io.github.kamiazya.scopes.scopemanagement.application.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryEvaluator
import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Use case for filtering scopes using advanced aspect queries.
 * Supports comparison operators (=, !=, >, >=, <, <=) and logical operators (AND, OR, NOT).
 */
class FilterScopesWithQueryUseCase(
    private val scopeRepository: ScopeRepository,
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val parser: AspectQueryParser = AspectQueryParser(),
) : UseCase<FilterScopesWithQueryUseCase.Query, ScopesError, List<Scope>> {

    data class Query(val query: String, val parentId: String? = null, val offset: Int = 0, val limit: Int = 100)

    /**
     * Filter scopes using an aspect query.
     * @param input Query containing the query string and optional parent ID
     * @return List of scopes matching the query
     */
    override suspend operator fun invoke(input: Query): Either<ScopesError, List<Scope>> {
        // Parse the query
        val ast = parser.parse(input.query).fold(
            { error ->
                return ScopesError.InvalidOperation("Invalid query: ${formatParseError(error)}").left()
            },
            { it },
        )

        // Get all aspect definitions for type-aware comparison
        val definitionsResult = aspectDefinitionRepository.findAll()
        val definitions = when (definitionsResult) {
            is Either.Left -> return ScopesError.SystemError("Failed to load aspect definitions").left()
            is Either.Right -> definitionsResult.value.associateBy { it.key.value }
        }

        // Create evaluator with definitions
        val evaluator = AspectQueryEvaluator(definitions)

        // Get scopes to filter
        val scopesToFilter = when {
            input.parentId != null -> {
                val parentScopeId = ScopeId.create(input.parentId).fold(
                    { return ScopesError.InvalidOperation("Invalid parent ID: ${input.parentId}").left() },
                    { it },
                )
                scopeRepository.findByParentId(parentScopeId).fold(
                    { return ScopesError.SystemError("Failed to load scopes: $it").left() },
                    { it },
                )
            }
            input.offset > 0 || input.limit < 100 -> {
                // Use pagination - get all scopes with offset and limit
                scopeRepository.findAll(input.offset, input.limit).fold(
                    { return ScopesError.SystemError("Failed to load scopes: $it").left() },
                    { it },
                )
            }
            else -> {
                // Default behavior - get root scopes only
                scopeRepository.findAllRoot().fold(
                    { return ScopesError.SystemError("Failed to load scopes: $it").left() },
                    { it },
                )
            }
        }

        // Filter scopes based on the query
        val filteredScopes = scopesToFilter.filter { scope ->
            evaluator.evaluate(ast, scope.aspects)
        }

        return filteredScopes.right()
    }

    /**
     * Filter all scopes (not just root or children) using an aspect query.
     * @param query The query string
     * @param offset Number of items to skip
     * @param limit Maximum number of items to return
     * @return List of scopes matching the query
     */
    suspend fun executeAll(query: String, offset: Int = 0, limit: Int = 100): Either<ScopesError, List<Scope>> {
        // Parse the query
        val ast = parser.parse(query).fold(
            { error ->
                return ScopesError.InvalidOperation("Invalid query: ${formatParseError(error)}").left()
            },
            { it },
        )

        // Get all aspect definitions
        val definitionsResult = aspectDefinitionRepository.findAll()
        val definitions = when (definitionsResult) {
            is Either.Left -> return ScopesError.SystemError("Failed to load aspect definitions").left()
            is Either.Right -> definitionsResult.value.associateBy { it.key.value }
        }

        val evaluator = AspectQueryEvaluator(definitions)

        // Get all scopes - this might need pagination for large datasets
        val allScopes = scopeRepository.findAll(offset, limit).fold(
            { return ScopesError.SystemError("Failed to load scopes: $it").left() },
            { it },
        )

        // Filter scopes
        val filteredScopes = allScopes.filter { scope ->
            evaluator.evaluate(ast, scope.aspects)
        }

        return filteredScopes.right()
    }

    private fun formatParseError(error: io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError): String = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.EmptyQuery ->
            "Query cannot be empty"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.UnexpectedCharacter ->
            "Unexpected character '${error.char}' at position ${error.position}"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.UnterminatedString ->
            "Unterminated string at position ${error.position}"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.UnexpectedToken ->
            "Unexpected token at position ${error.position}"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.MissingClosingParen ->
            "Missing closing parenthesis at position ${error.position}"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.ExpectedExpression ->
            "Expected expression at position ${error.position}"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.ExpectedIdentifier ->
            "Expected identifier at position ${error.position}"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.ExpectedOperator ->
            "Expected operator at position ${error.position}"
        is io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError.ExpectedValue ->
            "Expected value at position ${error.position}"
    }
}
