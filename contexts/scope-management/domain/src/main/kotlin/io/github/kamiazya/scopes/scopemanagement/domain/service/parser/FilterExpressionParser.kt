package io.github.kamiazya.scopes.scopemanagement.domain.service.parser

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
            val result = processNextToken(expression, position, tokens)
            when (result) {
                is TokenResult.Success -> position = result.newPosition
                is TokenResult.Error -> return result.error.left()
            }
        }

        return tokens.right()
    }

    private fun processNextToken(expression: String, position: Int, tokens: MutableList<Token>): TokenResult {
        val char = expression[position]

        return when {
            char.isWhitespace() -> TokenResult.Success(position + 1)
            char == '(' -> processParenthesis(position, tokens, Token.LeftParen(position))
            char == ')' -> processParenthesis(position, tokens, Token.RightParen(position))
            char == '\'' || char == '"' -> processStringLiteral(expression, position, tokens)
            else -> processOperatorOrKeyword(expression, position, tokens)
        }
    }

    private fun processParenthesis(position: Int, tokens: MutableList<Token>, token: Token): TokenResult {
        tokens.add(token)
        return TokenResult.Success(position + 1)
    }

    private fun processStringLiteral(expression: String, position: Int, tokens: MutableList<Token>): TokenResult {
        val quote = expression[position]
        val start = position
        var currentPos = position + 1

        while (currentPos < expression.length && expression[currentPos] != quote) {
            currentPos++
        }

        if (currentPos >= expression.length) {
            return TokenResult.Error(
                ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.UnterminatedString(start),
                ),
            )
        }

        val value = expression.substring(start + 1, currentPos)
        tokens.add(Token.StringLiteral(value, start))
        return TokenResult.Success(currentPos + 1)
    }

    private fun processOperatorOrKeyword(expression: String, position: Int, tokens: MutableList<Token>): TokenResult {
        val remaining = expression.substring(position)

        return when {
            remaining.startsWith("==") -> processDoubleCharOperator(position, tokens, ComparisonOperator.EQUALS)
            remaining.startsWith("!=") -> processDoubleCharOperator(position, tokens, ComparisonOperator.NOT_EQUALS)
            remaining.startsWith(">=") -> processDoubleCharOperator(position, tokens, ComparisonOperator.GREATER_THAN_OR_EQUAL)
            remaining.startsWith("<=") -> processDoubleCharOperator(position, tokens, ComparisonOperator.LESS_THAN_OR_EQUAL)
            expression[position] == '>' -> processSingleCharOperator(position, tokens, ComparisonOperator.GREATER_THAN)
            expression[position] == '<' -> processSingleCharOperator(position, tokens, ComparisonOperator.LESS_THAN)
            expression[position] == ':' -> processSingleCharOperator(position, tokens, ComparisonOperator.EQUALS)
            expression[position] == '=' -> processSingleCharOperator(position, tokens, ComparisonOperator.EQUALS)
            else -> processKeywordOrIdentifier(expression, position, tokens)
        }
    }

    private fun processDoubleCharOperator(position: Int, tokens: MutableList<Token>, operator: ComparisonOperator): TokenResult {
        tokens.add(Token.Operator(operator, position))
        return TokenResult.Success(position + 2)
    }

    private fun processSingleCharOperator(position: Int, tokens: MutableList<Token>, operator: ComparisonOperator): TokenResult {
        tokens.add(Token.Operator(operator, position))
        return TokenResult.Success(position + 1)
    }

    private fun processKeywordOrIdentifier(expression: String, position: Int, tokens: MutableList<Token>): TokenResult {
        val remaining = expression.substring(position).uppercase()

        return when {
            remaining.startsWith("AND") && isWordBoundary(expression, position, 3) -> {
                tokens.add(Token.And(position))
                TokenResult.Success(position + 3)
            }
            remaining.startsWith("OR") && isWordBoundary(expression, position, 2) -> {
                tokens.add(Token.Or(position))
                TokenResult.Success(position + 2)
            }
            remaining.startsWith("NOT") && isWordBoundary(expression, position, 3) -> {
                tokens.add(Token.Not(position))
                TokenResult.Success(position + 3)
            }
            isIdentifierStart(expression[position]) -> processIdentifier(expression, position, tokens)
            else -> TokenResult.Error(
                ContextError.InvalidFilterSyntax(
                    expression = expression,
                    errorType = ContextError.FilterSyntaxErrorType.UnexpectedCharacter(expression[position], position),
                ),
            )
        }
    }

    private fun processIdentifier(expression: String, position: Int, tokens: MutableList<Token>): TokenResult {
        val start = position
        var currentPos = position

        while (currentPos < expression.length && isIdentifierChar(expression[currentPos])) {
            currentPos++
        }

        val value = expression.substring(start, currentPos)
        tokens.add(Token.Identifier(value, start))
        return TokenResult.Success(currentPos)
    }

    private fun isWordBoundary(expression: String, position: Int, length: Int): Boolean =
        position + length >= expression.length || !expression[position + length].isLetterOrDigit()

    private fun isIdentifierStart(char: Char): Boolean = char.isLetterOrDigit() || char == '_'

    private fun isIdentifierChar(char: Char): Boolean = char.isLetterOrDigit() || char == '_'

    private sealed class TokenResult {
        data class Success(val newPosition: Int) : TokenResult()
        data class Error(val error: ContextError) : TokenResult()
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

                    if (!hasMoreTokens()) {
                        error("Expected value after operator")
                    }

                    val value = when (val valueToken = tokens[position]) {
                        is Token.StringLiteral -> {
                            position++
                            valueToken.value
                        }
                        is Token.Identifier -> {
                            // Allow unquoted identifiers as values (e.g., status:active)
                            position++
                            valueToken.value
                        }
                        else -> error("Expected value after operator, but got: $valueToken")
                    }

                    return FilterExpressionAST.Comparison(key, operator, value)
                }

                else -> error("Unexpected token: $token")
            }
        }
    }
}
