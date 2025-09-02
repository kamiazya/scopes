package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

/**
 * Tokens used in filter expression parsing.
 * Each token represents a meaningful unit in the filter expression.
 */
sealed class Token {
    abstract val position: Int

    data class Identifier(val value: String, override val position: Int) : Token()
    data class StringLiteral(val value: String, override val position: Int) : Token()
    data class Operator(val op: ComparisonOperator, override val position: Int) : Token()
    data class And(override val position: Int) : Token()
    data class Or(override val position: Int) : Token()
    data class Not(override val position: Int) : Token()
    data class LeftParen(override val position: Int) : Token()
    data class RightParen(override val position: Int) : Token()
}
