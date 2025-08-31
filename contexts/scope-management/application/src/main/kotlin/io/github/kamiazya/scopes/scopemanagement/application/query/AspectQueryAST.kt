package io.github.kamiazya.scopes.scopemanagement.application.query

/**
 * Abstract Syntax Tree for aspect queries.
 * Represents the structure of a parsed query expression.
 */
sealed class AspectQueryAST {
    /**
     * Comparison expression: key operator value
     * Examples: priority=high, age>=18, status!=closed
     */
    data class Comparison(val key: String, val operator: ComparisonOperator, val value: String) : AspectQueryAST()

    /**
     * Logical AND expression
     * Example: priority=high AND status=open
     */
    data class And(val left: AspectQueryAST, val right: AspectQueryAST) : AspectQueryAST()

    /**
     * Logical OR expression
     * Example: priority=high OR priority=medium
     */
    data class Or(val left: AspectQueryAST, val right: AspectQueryAST) : AspectQueryAST()

    /**
     * Logical NOT expression
     * Example: NOT status=closed
     */
    data class Not(val expression: AspectQueryAST) : AspectQueryAST()

    /**
     * Parenthesized expression for grouping
     * Example: (priority=high OR priority=medium) AND status=open
     */
    data class Parentheses(val expression: AspectQueryAST) : AspectQueryAST()
}

/**
 * Comparison operators supported in queries.
 */
enum class ComparisonOperator(val symbol: String) {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUALS(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    ;

    companion object {
        fun fromSymbol(symbol: String): ComparisonOperator? = values().find { it.symbol == symbol }
    }
}
