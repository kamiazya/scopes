package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ComparisonOperator
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.FilterExpressionAST
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Token

/**
 * Parser for filter expressions.
 * Converts filter expression strings into AST representations.
 */
class FilterExpressionParser {

    /**
     * Parse a filter expression into an AST.
     * @param expression The filter expression string
     * @return Either a parse error or the parsed AST
     */
    fun parse(expression: String): Either<ContextError, FilterExpressionAST> {
        val tokens = tokenize(expression).fold(
            { return it.left() },
            { it },
        )

        if (tokens.isEmpty()) {
            return ContextError.InvalidFilterSyntax(
                expression = expression,
                errorType = ContextError.FilterSyntaxErrorType.EmptyExpression,
            ).left()
        }

        return try {
            val parser = Parser(tokens)
            val ast = parser.parseExpression()

            if (parser.hasMoreTokens()) {
                ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.UnexpectedToken(parser.currentPosition()),
                ).left()
            } else {
                ast.right()
            }
        } catch (e: Exception) {
            ContextError.InvalidFilterSyntax(
                expression = expression,
                errorType = ContextError.FilterSyntaxErrorType.InvalidSyntax,
            ).left()
        }
    }

    private fun tokenize(expression: String): Either<ContextError, List<Token>> {
        val tokens = mutableListOf<Token>()
        var position = 0

        while (position < expression.length) {
            when {
                expression[position].isWhitespace() -> position++

                expression[position] == '(' -> {
                    tokens.add(Token.LeftParen(position))
                    position++
                }

                expression[position] == ')' -> {
                    tokens.add(Token.RightParen(position))
                    position++
                }

                expression[position] == '\'' || expression[position] == '"' -> {
                    val quote = expression[position]
                    val start = position++

                    while (position < expression.length && expression[position] != quote) {
                        position++
                    }

                    if (position >= expression.length) {
                        return ContextError.InvalidFilterSyntax(
                            expression = expression,
                            errorType = ContextError.FilterSyntaxErrorType.UnterminatedString(start),
                        ).left()
                    }

                    val value = expression.substring(start + 1, position)
                    tokens.add(Token.StringLiteral(value, start))
                    position++ // Skip closing quote
                }

                expression.substring(position).startsWith("==") -> {
                    tokens.add(Token.Operator(ComparisonOperator.EQUALS, position))
                    position += 2
                }

                expression.substring(position).startsWith("!=") -> {
                    tokens.add(Token.Operator(ComparisonOperator.NOT_EQUALS, position))
                    position += 2
                }

                expression.substring(position).startsWith(">=") -> {
                    tokens.add(Token.Operator(ComparisonOperator.GREATER_THAN_OR_EQUAL, position))
                    position += 2
                }

                expression.substring(position).startsWith("<=") -> {
                    tokens.add(Token.Operator(ComparisonOperator.LESS_THAN_OR_EQUAL, position))
                    position += 2
                }

                expression[position] == '>' -> {
                    tokens.add(Token.Operator(ComparisonOperator.GREATER_THAN, position))
                    position++
                }

                expression[position] == '<' -> {
                    tokens.add(Token.Operator(ComparisonOperator.LESS_THAN, position))
                    position++
                }

                expression.substring(position).uppercase().startsWith("AND") &&
                    (position + 3 >= expression.length || !expression[position + 3].isLetterOrDigit()) -> {
                    tokens.add(Token.And(position))
                    position += 3
                }

                expression.substring(position).uppercase().startsWith("OR") &&
                    (position + 2 >= expression.length || !expression[position + 2].isLetterOrDigit()) -> {
                    tokens.add(Token.Or(position))
                    position += 2
                }

                expression.substring(position).uppercase().startsWith("NOT") &&
                    (position + 3 >= expression.length || !expression[position + 3].isLetterOrDigit()) -> {
                    tokens.add(Token.Not(position))
                    position += 3
                }

                expression[position].isLetterOrDigit() || expression[position] == '_' -> {
                    val start = position

                    while (position < expression.length &&
                        (expression[position].isLetterOrDigit() || expression[position] == '_')
                    ) {
                        position++
                    }

                    val value = expression.substring(start, position)
                    tokens.add(Token.Identifier(value, start))
                }

                else -> {
                    return ContextError.InvalidFilterSyntax(
                        expression = expression,
                        errorType = ContextError.FilterSyntaxErrorType.UnexpectedCharacter(expression[position], position),
                    ).left()
                }
            }
        }

        return tokens.right()
    }

    private class Parser(private val tokens: List<Token>) {
        private var position = 0

        fun hasMoreTokens(): Boolean = position < tokens.size

        fun currentPosition(): Int = if (position < tokens.size) tokens[position].position else 0

        fun parseExpression(): FilterExpressionAST = parseOr()

        private fun parseOr(): FilterExpressionAST {
            var left = parseAnd()

            while (hasMoreTokens() && tokens[position] is Token.Or) {
                position++
                val right = parseAnd()
                left = FilterExpressionAST.Or(left, right)
            }

            return left
        }

        private fun parseAnd(): FilterExpressionAST {
            var left = parseNot()

            while (hasMoreTokens() && tokens[position] is Token.And) {
                position++
                val right = parseNot()
                left = FilterExpressionAST.And(left, right)
            }

            return left
        }

        private fun parseNot(): FilterExpressionAST {
            if (hasMoreTokens() && tokens[position] is Token.Not) {
                position++
                return FilterExpressionAST.Not(parseNot())
            }

            return parsePrimary()
        }

        private fun parsePrimary(): FilterExpressionAST {
            if (!hasMoreTokens()) {
                error("Unexpected end of expression")
            }

            when (val token = tokens[position]) {
                is Token.LeftParen -> {
                    position++
                    val expr = parseExpression()

                    if (!hasMoreTokens() || tokens[position] !is Token.RightParen) {
                        error("Expected closing parenthesis")
                    }

                    position++
                    return FilterExpressionAST.Parentheses(expr)
                }

                is Token.Identifier -> {
                    val key = token.value
                    position++

                    if (!hasMoreTokens() || tokens[position] !is Token.Operator) {
                        error("Expected operator after identifier")
                    }

                    val operator = (tokens[position] as Token.Operator).op
                    position++

                    if (!hasMoreTokens() || tokens[position] !is Token.StringLiteral) {
                        error("Expected value after operator")
                    }

                    val value = (tokens[position] as Token.StringLiteral).value
                    position++

                    return FilterExpressionAST.Comparison(key, operator, value)
                }

                else -> error("Unexpected token: $token")
            }
        }
    }
}
