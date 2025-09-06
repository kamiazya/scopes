package io.github.kamiazya.scopes.scopemanagement.domain.service.query

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.QueryParseError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.query.AspectQueryAST

/**
 * Domain service for parsing and evaluating aspect queries.
 * Encapsulates the business logic for query processing.
 */
class AspectQueryService {
    private val parser = AspectQueryParser()

    /**
     * Parse a query string into an AST.
     */
    fun parseQuery(queryString: String): Either<QueryParseError, AspectQueryAST> = parser.parse(queryString)

    /**
     * Create an evaluator with the given aspect definitions.
     * The evaluator can be used to evaluate multiple aspect sets against the same query.
     */
    fun createEvaluator(aspectDefinitions: Map<String, AspectDefinition>): AspectQueryEvaluator = AspectQueryEvaluator(aspectDefinitions)

    /**
     * Evaluate if the given aspects match the query.
     * This is a convenience method that creates an evaluator and evaluates in one step.
     */
    fun evaluate(query: AspectQueryAST, aspects: Aspects, aspectDefinitions: Map<String, AspectDefinition>): Boolean {
        val evaluator = createEvaluator(aspectDefinitions)
        return evaluator.evaluate(query, aspects)
    }
}
