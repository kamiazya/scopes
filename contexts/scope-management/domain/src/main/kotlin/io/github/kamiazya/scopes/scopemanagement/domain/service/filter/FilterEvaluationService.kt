package io.github.kamiazya.scopes.scopemanagement.domain.service.filter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.service.parser.FilterExpressionParser
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ComparisonOperator
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.FilterExpressionAST

/**
 * Domain service for evaluating context view filters against scopes.
 * This service encapsulates the logic for filter evaluation, keeping it
 * within the domain layer while maintaining separation of concerns.
 *
 * The service requires aspect definitions to perform type-aware comparisons.
 *
 * @param caseSensitiveComparisons Whether string comparisons should be case sensitive by default
 */
class FilterEvaluationService(private val caseSensitiveComparisons: Boolean = false) {

    private val parser = FilterExpressionParser()

    /**
     * Evaluate if a scope matches the given filter.
     *
     * @param filter The context view filter to evaluate
     * @param scope The scope to check against the filter
     * @param aspectDefinitions Map of aspect definitions for type-aware comparison
     * @return Either an error or boolean result
     */
    fun evaluateScope(filter: ContextViewFilter, scope: Scope, aspectDefinitions: Map<String, AspectDefinition>): Either<ContextError, Boolean> =
        evaluateAspects(filter, scope.aspects, aspectDefinitions)

    /**
     * Evaluate if aspects match the given filter.
     * This is a lower-level method that works directly with aspects.
     *
     * @param filter The context view filter to evaluate
     * @param aspects The aspects to check against the filter
     * @param aspectDefinitions Map of aspect definitions for type-aware comparison
     * @return Either an error or boolean result
     */
    fun evaluateAspects(filter: ContextViewFilter, aspects: Aspects, aspectDefinitions: Map<String, AspectDefinition>): Either<ContextError, Boolean> {
        // Parse the filter expression
        val ast = parser.parse(filter.expression).fold(
            { error -> return error.left() },
            { it },
        )

        // Convert Aspects to a map for evaluation
        val aspectMap = aspects.toMap().mapKeys { it.key.value }
            .mapValues { it.value.toList() }

        // Evaluate the parsed AST
        return evaluateNode(ast, aspectMap, aspectDefinitions).right()
    }

    private fun evaluateNode(node: FilterExpressionAST, aspectMap: Map<String, List<AspectValue>>, aspectDefinitions: Map<String, AspectDefinition>): Boolean =
        when (node) {
            is FilterExpressionAST.Comparison -> evaluateComparison(node, aspectMap, aspectDefinitions)
            is FilterExpressionAST.And -> evaluateNode(node.left, aspectMap, aspectDefinitions) &&
                evaluateNode(node.right, aspectMap, aspectDefinitions)
            is FilterExpressionAST.Or -> evaluateNode(node.left, aspectMap, aspectDefinitions) ||
                evaluateNode(node.right, aspectMap, aspectDefinitions)
            is FilterExpressionAST.Not -> !evaluateNode(node.expression, aspectMap, aspectDefinitions)
            is FilterExpressionAST.Parentheses -> evaluateNode(node.expression, aspectMap, aspectDefinitions)
        }

    private fun evaluateComparison(
        comparison: FilterExpressionAST.Comparison,
        aspectMap: Map<String, List<AspectValue>>,
        aspectDefinitions: Map<String, AspectDefinition>,
    ): Boolean {
        val aspectValues = aspectMap[comparison.key] ?: return false
        val definition = aspectDefinitions[comparison.key]

        // For each value, check if it matches the comparison
        return aspectValues.any { aspectValue ->
            evaluateValueComparison(
                aspectValue,
                comparison.operator,
                comparison.value,
                definition,
            )
        }
    }

    private fun evaluateValueComparison(actualValue: AspectValue, operator: ComparisonOperator, expectedValue: String, definition: AspectDefinition?): Boolean {
        // If no definition, fall back to string comparison
        if (definition == null) {
            return evaluateStringComparison(actualValue.value, operator, expectedValue)
        }

        return when (definition.type) {
            is AspectType.Numeric -> evaluateNumericComparison(actualValue, operator, expectedValue)
            is AspectType.BooleanType -> evaluateBooleanComparison(actualValue, operator, expectedValue)
            is AspectType.Ordered -> evaluateOrderedComparison(actualValue, operator, expectedValue, definition)
            is AspectType.Text -> evaluateStringComparison(actualValue.value, operator, expectedValue)
            is AspectType.Duration -> evaluateDurationComparison(actualValue, operator, expectedValue)
        }
    }

    private fun evaluateStringComparison(actual: String, operator: ComparisonOperator, expected: String): Boolean = when (operator) {
        ComparisonOperator.EQUALS -> actual == expected
        ComparisonOperator.NOT_EQUALS -> actual != expected
        ComparisonOperator.GREATER_THAN -> actual > expected
        ComparisonOperator.GREATER_THAN_OR_EQUAL -> actual >= expected
        ComparisonOperator.LESS_THAN -> actual < expected
        ComparisonOperator.LESS_THAN_OR_EQUAL -> actual <= expected
        ComparisonOperator.CONTAINS -> actual.contains(expected, ignoreCase = !caseSensitiveComparisons)
        ComparisonOperator.NOT_CONTAINS -> !actual.contains(expected, ignoreCase = !caseSensitiveComparisons)
    }

    private fun evaluateNumericComparison(actualValue: AspectValue, operator: ComparisonOperator, expectedValue: String): Boolean {
        val actual = actualValue.toNumericValue() ?: return false
        val expected = expectedValue.toDoubleOrNull() ?: return false

        return when (operator) {
            ComparisonOperator.EQUALS -> actual == expected
            ComparisonOperator.NOT_EQUALS -> actual != expected
            ComparisonOperator.GREATER_THAN -> actual > expected
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> actual >= expected
            ComparisonOperator.LESS_THAN -> actual < expected
            ComparisonOperator.LESS_THAN_OR_EQUAL -> actual <= expected
            ComparisonOperator.CONTAINS -> false // Numeric values don't support contains
            ComparisonOperator.NOT_CONTAINS -> true // Numeric values don't support contains
        }
    }

    private fun evaluateBooleanComparison(actualValue: AspectValue, operator: ComparisonOperator, expectedValue: String): Boolean {
        val actual = actualValue.toBooleanValue() ?: return false
        val expected = when (expectedValue.lowercase()) {
            "true", "yes", "1" -> true
            "false", "no", "0" -> false
            else -> return false
        }

        return when (operator) {
            ComparisonOperator.EQUALS -> actual == expected
            ComparisonOperator.NOT_EQUALS -> actual != expected
            // Other operators don't make sense for booleans
            else -> false
        }
    }

    private fun evaluateOrderedComparison(
        actualValue: AspectValue,
        operator: ComparisonOperator,
        expectedValue: String,
        definition: AspectDefinition,
    ): Boolean {
        // Parse expected value to AspectValue
        val expectedAspectValue = AspectValue.create(expectedValue).getOrNull() ?: return false

        // Get the order indices
        val actualOrder = definition.getValueOrder(actualValue) ?: return false
        val expectedOrder = definition.getValueOrder(expectedAspectValue) ?: return false

        return when (operator) {
            ComparisonOperator.EQUALS -> actualOrder == expectedOrder
            ComparisonOperator.NOT_EQUALS -> actualOrder != expectedOrder
            ComparisonOperator.GREATER_THAN -> actualOrder > expectedOrder
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> actualOrder >= expectedOrder
            ComparisonOperator.LESS_THAN -> actualOrder < expectedOrder
            ComparisonOperator.LESS_THAN_OR_EQUAL -> actualOrder <= expectedOrder
            ComparisonOperator.CONTAINS -> false // Ordered values don't support contains
            ComparisonOperator.NOT_CONTAINS -> true // Ordered values don't support contains
        }
    }

    private fun evaluateDurationComparison(actualValue: AspectValue, operator: ComparisonOperator, expectedValue: String): Boolean {
        val actual = actualValue.parseDuration() ?: return false

        // Parse expected value as AspectValue first, then as Duration
        val expectedAspectValue = AspectValue.create(expectedValue).getOrNull() ?: return false
        val expected = expectedAspectValue.parseDuration() ?: return false

        return when (operator) {
            ComparisonOperator.EQUALS -> actual == expected
            ComparisonOperator.NOT_EQUALS -> actual != expected
            ComparisonOperator.GREATER_THAN -> actual > expected
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> actual >= expected
            ComparisonOperator.LESS_THAN -> actual < expected
            ComparisonOperator.LESS_THAN_OR_EQUAL -> actual <= expected
            ComparisonOperator.CONTAINS -> false // Duration values don't support contains
            ComparisonOperator.NOT_CONTAINS -> true // Duration values don't support contains
        }
    }
}
