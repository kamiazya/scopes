package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.valueobject.AspectCriteria
import io.github.kamiazya.scopes.domain.valueobject.AspectCriterion
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.ComparisonOperator
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter

/**
 * Service for parsing filter expressions into AspectCriteria.
 * Supports expressions like: "project=acme AND priority>=medium OR type=bug"
 *
 * Grammar:
 * - Expression: Term (AND|OR Term)*
 * - Term: Key Operator Value | (Expression)
 * - Key: [a-zA-Z][a-zA-Z0-9_-]*
 * - Operator: = | != | > | >= | < | <= | CONTAINS | NOT_CONTAINS
 * - Value: quoted string | unquoted word
 */
class FilterExpressionParser {

    private data class Token(val type: TokenType, val value: String, val position: Int)

    private enum class TokenType {
        KEY,
        OPERATOR,
        VALUE,
        AND,
        OR,
        LEFT_PAREN,
        RIGHT_PAREN,
        EOF,
    }

    /**
     * Parse a filter expression string into a ContextFilter.
     */
    fun parse(expression: String): Either<String, ContextViewFilter> {
        if (expression.trim().isEmpty() || expression.trim() == "*") {
            return ContextViewFilter.all().right()
        }

        // TODO: Fix this to properly create ContextViewFilter
        // For now, just use the create method directly
        return ContextViewFilter.create(expression).mapLeft { error ->
            "Parse error: Invalid filter expression"
        }
    }

    private fun tokenize(expression: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < expression.length) {
            when {
                expression[i].isWhitespace() -> {
                    i++
                }
                expression[i] == '(' -> {
                    tokens.add(Token(TokenType.LEFT_PAREN, "(", i))
                    i++
                }
                expression[i] == ')' -> {
                    tokens.add(Token(TokenType.RIGHT_PAREN, ")", i))
                    i++
                }
                expression[i] == '"' -> {
                    // Quoted value
                    val start = i++
                    while (i < expression.length && expression[i] != '"') i++
                    if (i >= expression.length) throw ParseException("Unterminated quoted string")
                    val value = expression.substring(start + 1, i)
                    tokens.add(Token(TokenType.VALUE, value, start))
                    i++
                }
                expression.startsWith("AND", i, ignoreCase = true) -> {
                    tokens.add(Token(TokenType.AND, "AND", i))
                    i += 3
                }
                expression.startsWith("OR", i, ignoreCase = true) -> {
                    tokens.add(Token(TokenType.OR, "OR", i))
                    i += 2
                }
                expression.startsWith("NOT_CONTAINS", i, ignoreCase = true) -> {
                    tokens.add(Token(TokenType.OPERATOR, "NOT_CONTAINS", i))
                    i += 12
                }
                expression.startsWith("CONTAINS", i, ignoreCase = true) -> {
                    tokens.add(Token(TokenType.OPERATOR, "CONTAINS", i))
                    i += 8
                }
                expression.startsWith(">=", i) -> {
                    tokens.add(Token(TokenType.OPERATOR, ">=", i))
                    i += 2
                }
                expression.startsWith("<=", i) -> {
                    tokens.add(Token(TokenType.OPERATOR, "<=", i))
                    i += 2
                }
                expression.startsWith("!=", i) -> {
                    tokens.add(Token(TokenType.OPERATOR, "!=", i))
                    i += 2
                }
                expression[i] in "=><" -> {
                    tokens.add(Token(TokenType.OPERATOR, expression[i].toString(), i))
                    i++
                }
                expression[i].isLetter() -> {
                    // Key or unquoted value
                    val start = i
                    while (i < expression.length && (expression[i].isLetterOrDigit() || expression[i] in "_-")) {
                        i++
                    }
                    val value = expression.substring(start, i)

                    // Determine if this is a key or value based on context
                    val lastToken = tokens.lastOrNull()
                    val tokenType = when {
                        lastToken?.type == TokenType.OPERATOR -> TokenType.VALUE
                        lastToken?.type == null ||
                            lastToken.type == TokenType.AND ||
                            lastToken.type == TokenType.OR ||
                            lastToken.type == TokenType.LEFT_PAREN -> TokenType.KEY
                        else -> TokenType.VALUE
                    }

                    tokens.add(Token(tokenType, value, start))
                }
                else -> {
                    throw ParseException("Unexpected character '${expression[i]}' at position $i")
                }
            }
        }

        tokens.add(Token(TokenType.EOF, "", expression.length))
        return tokens
    }

    private fun parseExpression(tokens: Iterator<Token>): AspectCriteria {
        var left = parseTerm(tokens)

        while (tokens.hasNext()) {
            val token = tokens.next()
            when (token.type) {
                TokenType.AND -> {
                    val right = parseTerm(tokens)
                    left = AspectCriteria.and(left, right)
                }
                TokenType.OR -> {
                    val right = parseTerm(tokens)
                    left = AspectCriteria.or(left, right)
                }
                TokenType.RIGHT_PAREN, TokenType.EOF -> {
                    // Put the token back (conceptually)
                    break
                }
                else -> throw ParseException("Expected AND, OR, or end of expression but got ${token.value}")
            }
        }

        return left
    }

    private fun parseTerm(tokens: Iterator<Token>): AspectCriteria {
        if (!tokens.hasNext()) throw ParseException("Unexpected end of expression")

        val token = tokens.next()

        return when (token.type) {
            TokenType.LEFT_PAREN -> {
                val expr = parseExpression(tokens)
                if (!tokens.hasNext() || tokens.next().type != TokenType.RIGHT_PAREN) {
                    throw ParseException("Expected closing parenthesis")
                }
                expr
            }
            TokenType.KEY -> {
                if (!tokens.hasNext()) throw ParseException("Expected operator after key '${token.value}'")
                val operatorToken = tokens.next()
                if (operatorToken.type != TokenType.OPERATOR) {
                    throw ParseException("Expected operator but got '${operatorToken.value}'")
                }

                if (!tokens.hasNext()) throw ParseException("Expected value after operator '${operatorToken.value}'")
                val valueToken = tokens.next()
                if (valueToken.type != TokenType.VALUE) {
                    throw ParseException("Expected value but got '${valueToken.value}'")
                }

                val key = AspectKey.create(token.value).fold(
                    ifLeft = { throw ParseException("Invalid aspect key: ${token.value}") },
                    ifRight = { it },
                )
                val value = AspectValue.create(valueToken.value).fold(
                    ifLeft = { throw ParseException("Invalid aspect value: ${valueToken.value}") },
                    ifRight = { it },
                )
                val operator = parseOperator(operatorToken.value)

                AspectCriteria.from(AspectCriterion(key, operator, value))
            }
            else -> throw ParseException("Expected key or opening parenthesis but got '${token.value}'")
        }
    }

    private fun parseOperator(operatorStr: String): ComparisonOperator = when (operatorStr) {
        "=" -> ComparisonOperator.EQUALS
        "!=" -> ComparisonOperator.NOT_EQUALS
        ">" -> ComparisonOperator.GREATER_THAN
        ">=" -> ComparisonOperator.GREATER_THAN_OR_EQUAL
        "<" -> ComparisonOperator.LESS_THAN
        "<=" -> ComparisonOperator.LESS_THAN_OR_EQUAL
        "CONTAINS" -> ComparisonOperator.CONTAINS
        "NOT_CONTAINS" -> ComparisonOperator.NOT_CONTAINS
        else -> throw ParseException("Unknown operator: $operatorStr")
    }

    private class ParseException(message: String) : Exception(message)
}
