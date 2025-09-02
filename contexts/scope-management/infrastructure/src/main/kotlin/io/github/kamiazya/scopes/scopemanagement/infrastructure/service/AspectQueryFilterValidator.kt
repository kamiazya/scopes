package io.github.kamiazya.scopes.scopemanagement.infrastructure.service

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.application.query.QueryParseError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.service.FilterExpressionValidator

/**
 * Infrastructure implementation of FilterExpressionValidator using AspectQueryParser.
 *
 * This implementation bridges the domain layer's need for filter validation
 * with the technical capability provided by AspectQueryParser.
 */
class AspectQueryFilterValidator(private val aspectQueryParser: AspectQueryParser) : FilterExpressionValidator {

    override fun validate(expression: String): Either<ContextError, Unit> = aspectQueryParser.parse(expression)
        .mapLeft { error ->
            // Map technical parsing errors to domain errors with type-safe error types
            when (error) {
                is QueryParseError.EmptyQuery -> ContextError.EmptyFilter
                is QueryParseError.UnexpectedCharacter -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.UnexpectedCharacter(
                        char = error.char,
                        position = error.position,
                    ),
                )
                is QueryParseError.UnterminatedString -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.UnterminatedString(
                        position = error.position,
                    ),
                )
                is QueryParseError.UnexpectedToken -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.UnexpectedToken(
                        position = error.position,
                    ),
                )
                is QueryParseError.MissingClosingParen -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.MissingClosingParen(
                        position = error.position,
                    ),
                )
                is QueryParseError.ExpectedExpression -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.ExpectedExpression(
                        position = error.position,
                    ),
                )
                is QueryParseError.ExpectedIdentifier -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.ExpectedIdentifier(
                        position = error.position,
                    ),
                )
                is QueryParseError.ExpectedOperator -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.ExpectedOperator(
                        position = error.position,
                    ),
                )
                is QueryParseError.ExpectedValue -> ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.ExpectedValue(
                        position = error.position,
                    ),
                )
            }
        }
        .map { Unit }
}
