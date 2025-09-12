package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors that can occur during query parsing.
 */
sealed class QueryParseError : ScopesError() {
    data object EmptyQuery : QueryParseError()
    data class UnexpectedCharacter(val char: Char, val position: Int) : QueryParseError()
    data class UnterminatedString(val position: Int) : QueryParseError()
    data class UnexpectedToken(val token: Any?, val position: Int) : QueryParseError()
    data class MissingClosingParen(val position: Int) : QueryParseError()
    data class ExpectedExpression(val position: Int) : QueryParseError()
    data class ExpectedIdentifier(val position: Int) : QueryParseError()
    data class ExpectedOperator(val position: Int) : QueryParseError()
    data class ExpectedValue(val position: Int) : QueryParseError()
}
