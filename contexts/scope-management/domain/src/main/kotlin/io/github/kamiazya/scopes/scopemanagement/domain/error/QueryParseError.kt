package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Errors that can occur during query parsing.
 */
sealed class QueryParseError(override val occurredAt: Instant = Clock.System.now()) : ScopesError() {
    object EmptyQuery : QueryParseError()
    data class UnexpectedCharacter(val char: Char, val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
    data class UnterminatedString(val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
    data class UnexpectedToken(val token: Any?, val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
    data class MissingClosingParen(val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
    data class ExpectedExpression(val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
    data class ExpectedIdentifier(val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
    data class ExpectedOperator(val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
    data class ExpectedValue(val position: Int, override val occurredAt: Instant = Clock.System.now()) : QueryParseError(occurredAt)
}
