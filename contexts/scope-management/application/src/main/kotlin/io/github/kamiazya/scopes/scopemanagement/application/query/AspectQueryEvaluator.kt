package io.github.kamiazya.scopes.scopemanagement.application.query

import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects

/**
 * Evaluates aspect query AST against a set of aspects.
 * Handles type-aware comparisons based on aspect definitions.
 */
class AspectQueryEvaluator(private val aspectDefinitions: Map<String, AspectDefinition>) {

    /**
     * Evaluate if the given aspects match the query.
     * @param query The parsed query AST
     * @param aspects The aspects to evaluate against
     * @return true if the aspects match the query
     */
    fun evaluate(query: AspectQueryAST, aspects: Aspects): Boolean {
        // Convert Aspects to a map for evaluation
        val aspectMap = aspects.toMap().mapKeys { it.key.value }
            .mapValues { it.value.toList() }

        return evaluateNode(query, aspectMap)
    }

    private fun evaluateNode(node: AspectQueryAST, aspectMap: Map<String, List<AspectValue>>): Boolean = when (node) {
        is AspectQueryAST.Comparison -> evaluateComparison(node, aspectMap)
        is AspectQueryAST.And -> evaluateNode(node.left, aspectMap) && evaluateNode(node.right, aspectMap)
        is AspectQueryAST.Or -> evaluateNode(node.left, aspectMap) || evaluateNode(node.right, aspectMap)
        is AspectQueryAST.Not -> !evaluateNode(node.expression, aspectMap)
        is AspectQueryAST.Parentheses -> evaluateNode(node.expression, aspectMap)
    }

    private fun evaluateComparison(comparison: AspectQueryAST.Comparison, aspectMap: Map<String, List<AspectValue>>): Boolean {
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
        ComparisonOperator.EQUALS -> actual.equals(expected, ignoreCase = true)
        ComparisonOperator.NOT_EQUALS -> !actual.equals(expected, ignoreCase = true)
        ComparisonOperator.GREATER_THAN -> actual > expected
        ComparisonOperator.GREATER_THAN_OR_EQUALS -> actual >= expected
        ComparisonOperator.LESS_THAN -> actual < expected
        ComparisonOperator.LESS_THAN_OR_EQUALS -> actual <= expected
    }

    private fun evaluateNumericComparison(actualValue: AspectValue, operator: ComparisonOperator, expectedValue: String): Boolean {
        val actual = actualValue.toNumericValue() ?: return false
        val expected = expectedValue.toDoubleOrNull() ?: return false

        return when (operator) {
            ComparisonOperator.EQUALS -> actual == expected
            ComparisonOperator.NOT_EQUALS -> actual != expected
            ComparisonOperator.GREATER_THAN -> actual > expected
            ComparisonOperator.GREATER_THAN_OR_EQUALS -> actual >= expected
            ComparisonOperator.LESS_THAN -> actual < expected
            ComparisonOperator.LESS_THAN_OR_EQUALS -> actual <= expected
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
            ComparisonOperator.GREATER_THAN_OR_EQUALS -> actualOrder >= expectedOrder
            ComparisonOperator.LESS_THAN -> actualOrder < expectedOrder
            ComparisonOperator.LESS_THAN_OR_EQUALS -> actualOrder <= expectedOrder
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
            ComparisonOperator.GREATER_THAN_OR_EQUALS -> actual >= expected
            ComparisonOperator.LESS_THAN -> actual < expected
            ComparisonOperator.LESS_THAN_OR_EQUALS -> actual <= expected
        }
    }
}
