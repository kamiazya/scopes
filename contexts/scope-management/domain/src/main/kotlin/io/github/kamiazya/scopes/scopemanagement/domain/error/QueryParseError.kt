package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Errors that can occur during query parsing.
 */
sealed class QueryParseError : ScopesError() {
    data class EmptyQuery(override val occurredAt: Instant) : QueryParseError()
    data class UnexpectedCharacter(val char: Char, val position: Int, override val occurredAt: Instant) : QueryParseError()
    data class UnterminatedString(val position: Int, override val occurredAt: Instant) : QueryParseError()
    data class UnexpectedToken(val token: Any?, val position: Int, override val occurredAt: Instant) : QueryParseError()
    data class MissingClosingParen(val position: Int, override val occurredAt: Instant) : QueryParseError()
    data class ExpectedExpression(val position: Int, override val occurredAt: Instant) : QueryParseError()
    data class ExpectedIdentifier(val position: Int, override val occurredAt: Instant) : QueryParseError()
    data class ExpectedOperator(val position: Int, override val occurredAt: Instant) : QueryParseError()
    data class ExpectedValue(val position: Int, override val occurredAt: Instant) : QueryParseError()
}
