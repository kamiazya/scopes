package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

/**
 * Abstract Syntax Tree representation for filter expressions.
 * This represents the parsed structure of a context view filter.
 */
sealed class FilterExpressionAST {
    /**
     * Comparison between an aspect key and a value.
     * Examples: "status == 'active'", "priority > 5"
     */
    data class Comparison(val key: String, val operator: ComparisonOperator, val value: String) : FilterExpressionAST()

    /**
     * Logical AND between two expressions.
     * Example: "status == 'active' AND priority > 5"
     */
    data class And(val left: FilterExpressionAST, val right: FilterExpressionAST) : FilterExpressionAST()

    /**
     * Logical OR between two expressions.
     * Example: "type == 'bug' OR type == 'feature'"
     */
    data class Or(val left: FilterExpressionAST, val right: FilterExpressionAST) : FilterExpressionAST()

    /**
     * Logical NOT of an expression.
     * Example: "NOT (status == 'closed')"
     */
    data class Not(val expression: FilterExpressionAST) : FilterExpressionAST()

    /**
     * Parentheses for grouping expressions.
     * Example: "(type == 'bug' OR type == 'feature') AND priority > 3"
     */
    data class Parentheses(val expression: FilterExpressionAST) : FilterExpressionAST()
}
