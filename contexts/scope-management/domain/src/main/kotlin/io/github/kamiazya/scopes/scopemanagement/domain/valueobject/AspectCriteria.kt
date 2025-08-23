package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.NonEmptyList
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Comparison operators for aspect queries.
 */
enum class ComparisonOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    CONTAINS,
    NOT_CONTAINS,
}

/**
 * Logical operators for combining aspect criteria.
 */
enum class LogicalOperator {
    AND,
    OR,
}

/**
 * Value object representing a single aspect criterion for filtering.
 */
data class AspectCriterion(val key: AspectKey, val operator: ComparisonOperator, val value: AspectValue) {
    companion object {
        /**
         * Create an equality criterion.
         */
        fun equals(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.EQUALS, value)

        /**
         * Create a not-equals criterion.
         */
        fun notEquals(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.NOT_EQUALS, value)

        /**
         * Create a greater-than criterion.
         */
        fun greaterThan(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.GREATER_THAN, value)

        /**
         * Create a greater-than-or-equal criterion.
         */
        fun greaterThanOrEqual(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.GREATER_THAN_OR_EQUAL, value)

        /**
         * Create a less-than criterion.
         */
        fun lessThan(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.LESS_THAN, value)

        /**
         * Create a less-than-or-equal criterion.
         */
        fun lessThanOrEqual(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.LESS_THAN_OR_EQUAL, value)

        /**
         * Create a contains criterion.
         */
        fun contains(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.CONTAINS, value)

        /**
         * Create a not-contains criterion.
         */
        fun notContains(key: AspectKey, value: AspectValue): AspectCriterion = AspectCriterion(key, ComparisonOperator.NOT_CONTAINS, value)
    }

    /**
     * Evaluate this criterion against an aspect value.
     * For backward compatibility, this method accepts a single value.
     */
    fun evaluate(actualValue: AspectValue?): Boolean {
        if (actualValue == null) return false

        return when (operator) {
            ComparisonOperator.EQUALS -> actualValue == value
            ComparisonOperator.NOT_EQUALS -> actualValue != value
            ComparisonOperator.CONTAINS -> actualValue.value.contains(value.value, ignoreCase = true)
            ComparisonOperator.NOT_CONTAINS -> !actualValue.value.contains(value.value, ignoreCase = true)
            ComparisonOperator.GREATER_THAN -> actualValue.value > value.value
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> actualValue.value >= value.value
            ComparisonOperator.LESS_THAN -> actualValue.value < value.value
            ComparisonOperator.LESS_THAN_OR_EQUAL -> actualValue.value <= value.value
        }
    }

    /**
     * Evaluate this criterion against multiple aspect values.
     * Returns true if ANY of the values match the criterion (OR logic).
     * For NOT_EQUALS, returns true only if ALL values don't match (AND logic).
     */
    fun evaluateMultiple(actualValues: List<AspectValue>?): Boolean {
        if (actualValues.isNullOrEmpty()) return false

        return when (operator) {
            ComparisonOperator.EQUALS ->
                // Any value equals the criterion value
                actualValues.any { it == value }
            ComparisonOperator.NOT_EQUALS ->
                // All values don't equal the criterion value
                actualValues.all { it != value }
            ComparisonOperator.CONTAINS ->
                // Any value contains the criterion value
                actualValues.any { it.value.contains(value.value, ignoreCase = true) }
            ComparisonOperator.NOT_CONTAINS ->
                // All values don't contain the criterion value
                actualValues.all { !it.value.contains(value.value, ignoreCase = true) }
            ComparisonOperator.GREATER_THAN ->
                // Any value is greater than the criterion value
                actualValues.any { it.value > value.value }
            ComparisonOperator.GREATER_THAN_OR_EQUAL ->
                // Any value is greater than or equal to the criterion value
                actualValues.any { it.value >= value.value }
            ComparisonOperator.LESS_THAN ->
                // Any value is less than the criterion value
                actualValues.any { it.value < value.value }
            ComparisonOperator.LESS_THAN_OR_EQUAL ->
                // Any value is less than or equal to the criterion value
                actualValues.any { it.value <= value.value }
        }
    }
}

/**
 * Value object representing complex aspect query criteria with logical operators.
 */
sealed class AspectCriteria {
    /**
     * Single criterion.
     */
    data class Single(val criterion: AspectCriterion) : AspectCriteria()

    /**
     * Combination of multiple criteria with logical operator.
     */
    data class Compound(val left: AspectCriteria, val operator: LogicalOperator, val right: AspectCriteria) : AspectCriteria()

    companion object {
        /**
         * Create criteria from a single criterion.
         */
        fun from(criterion: AspectCriterion): AspectCriteria = Single(criterion)

        /**
         * Combine two criteria with AND operator.
         */
        fun and(left: AspectCriteria, right: AspectCriteria): AspectCriteria = Compound(left, LogicalOperator.AND, right)

        /**
         * Combine two criteria with OR operator.
         */
        fun or(left: AspectCriteria, right: AspectCriteria): AspectCriteria = Compound(left, LogicalOperator.OR, right)
    }

    /**
     * Evaluate these criteria against a map of aspects.
     * Supports both single values (for backward compatibility) and multiple values.
     */
    fun evaluate(aspects: Map<AspectKey, AspectValue>): Boolean = when (this) {
        is Single -> criterion.evaluate(aspects[criterion.key])
        is Compound -> {
            val leftResult = left.evaluate(aspects)
            val rightResult = right.evaluate(aspects)
            when (operator) {
                LogicalOperator.AND -> leftResult && rightResult
                LogicalOperator.OR -> leftResult || rightResult
            }
        }
    }

    /**
     * Evaluate these criteria against aspects with multiple values.
     * This is the primary method for evaluating criteria with the new Aspects value object.
     */
    fun evaluateWithMultipleValues(aspects: Map<AspectKey, NonEmptyList<AspectValue>>): Boolean = when (this) {
        is Single -> criterion.evaluateMultiple(aspects[criterion.key]?.toList())
        is Compound -> {
            val leftResult = left.evaluateWithMultipleValues(aspects)
            val rightResult = right.evaluateWithMultipleValues(aspects)
            when (operator) {
                LogicalOperator.AND -> leftResult && rightResult
                LogicalOperator.OR -> leftResult || rightResult
            }
        }
    }

    /**
     * Evaluate these criteria against an Aspects value object.
     * Convenience method that delegates to evaluateWithMultipleValues.
     */
    fun evaluateWithAspects(aspects: Aspects): Boolean = evaluateWithMultipleValues(aspects.toMap())
}
