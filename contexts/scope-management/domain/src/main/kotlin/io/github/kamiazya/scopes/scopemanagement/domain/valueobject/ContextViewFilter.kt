package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
/**
 * Value object representing a filter definition for context views.
 * Filters determine which scopes are visible when a context view is active.
 *
 * The filter format uses a simple predicate syntax:
 * - Attribute comparisons: "priority == 'high'", "effort > 10"
 * - Logical operators: "status == 'in_progress' AND priority == 'high'"
 * - Complex conditions: "(type == 'bug' OR type == 'feature') AND !blocked"
 *
 * Supported operators:
 * - Comparison: ==, !=, <, >, <=, >=
 * - Logical: AND, OR, NOT (or !)
 * - Contains: CONTAINS, IN
 * - Existence: EXISTS, IS_NULL
 */
@JvmInline
value class ContextViewFilter private constructor(val expression: String) {

    companion object {
        private const val MAX_LENGTH = 1000
        private const val MIN_LENGTH = 1

        /**
         * Create a validated ContextViewFilter from a filter expression string.
         * Returns Either with validation error or valid filter.
         */
        fun create(expression: String): Either<ContextError, ContextViewFilter> = either {
            val trimmedExpression = expression.trim()

            ensure(trimmedExpression.isNotBlank()) { ContextError.EmptyFilter }
            ensure(trimmedExpression.length >= MIN_LENGTH) { ContextError.FilterTooShort(MIN_LENGTH) }
            ensure(trimmedExpression.length <= MAX_LENGTH) { ContextError.FilterTooLong(MAX_LENGTH) }

            // Basic syntax validation - only check for obvious issues
            // Advanced validation is done by FilterExpressionValidator
            val validationError = getBasicSyntaxError(trimmedExpression)
            ensure(validationError == null) {
                ContextError.InvalidFilterSyntax(
                    expression = trimmedExpression,
                    errorType = validationError ?: ContextError.FilterSyntaxErrorType.InvalidSyntax,
                )
            }

            ContextViewFilter(trimmedExpression)
        }

        /**
         * Perform basic syntax validation of the filter expression.
         * Returns error type if validation fails, null if valid.
         * This is a simplified validation - actual parsing happens during evaluation.
         */
        private fun getBasicSyntaxError(expression: String): ContextError.FilterSyntaxErrorType? {
            // Check for balanced parentheses
            var parenCount = 0
            for (char in expression) {
                when (char) {
                    '(' -> parenCount++
                    ')' -> parenCount--
                }
                if (parenCount < 0) return ContextError.FilterSyntaxErrorType.UnbalancedParentheses
            }
            if (parenCount != 0) return ContextError.FilterSyntaxErrorType.MissingClosingParen(expression.length)

            // Check for balanced quotes
            val singleQuoteCount = expression.count { it == '\'' }
            val doubleQuoteCount = expression.count { it == '"' }
            if (singleQuoteCount % 2 != 0 || doubleQuoteCount % 2 != 0) {
                return ContextError.FilterSyntaxErrorType.UnbalancedQuotes
            }

            // Check for empty operators (like "AND AND" or "OR OR")
            val operatorPattern = "\\b(AND|OR|NOT)\\s+(AND|OR|NOT)\\b".toRegex()
            if (operatorPattern.containsMatchIn(expression)) {
                return ContextError.FilterSyntaxErrorType.EmptyOperator
            }

            return null // No errors found
        }

        /**
         * Create a simple equality filter for a single attribute.
         */
        fun equals(attribute: String, value: String): ContextViewFilter = ContextViewFilter("$attribute == '$value'")

        /**
         * Create a simple non-equality filter for a single attribute.
         */
        fun notEquals(attribute: String, value: String): ContextViewFilter = ContextViewFilter("$attribute != '$value'")

        /**
         * Create a filter that checks if an attribute exists.
         */
        fun exists(attribute: String): ContextViewFilter = ContextViewFilter("EXISTS($attribute)")

        /**
         * Create a filter that checks if an attribute contains a value.
         */
        fun contains(attribute: String, value: String): ContextViewFilter = ContextViewFilter("$attribute CONTAINS '$value'")

        /**
         * Combine filters with AND logic.
         */
        fun and(filters: List<ContextViewFilter>): ContextViewFilter = ContextViewFilter(filters.joinToString(" AND ") { "(${it.expression})" })

        /**
         * Combine filters with OR logic.
         */
        fun or(filters: List<ContextViewFilter>): ContextViewFilter = ContextViewFilter(filters.joinToString(" OR ") { "(${it.expression})" })

        /**
         * Negate a filter.
         */
        fun not(filter: ContextViewFilter): ContextViewFilter = ContextViewFilter("NOT (${filter.expression})")
    }

    override fun toString(): String = expression
}
