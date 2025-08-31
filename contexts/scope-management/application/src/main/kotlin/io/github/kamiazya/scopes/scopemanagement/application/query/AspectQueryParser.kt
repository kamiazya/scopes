package io.github.kamiazya.scopes.scopemanagement.application.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Parser for aspect query expressions.
 * Supports comparison operators (=, !=, >, >=, <, <=) and logical operators (AND, OR, NOT).
 * Also supports parentheses for grouping expressions.
 *
 * Grammar:
 * ```
 * query        := or_expr
 * or_expr      := and_expr (OR and_expr)*
 * and_expr     := not_expr (AND not_expr)*
 * not_expr     := NOT not_expr | primary
 * primary      := comparison | '(' query ')'
 * comparison   := identifier operator value
 * operator     := '=' | '!=' | '>' | '>=' | '<' | '<='
 * identifier   := [a-zA-Z][a-zA-Z0-9_]*
 * value        := quoted_string | unquoted_value
 * quoted_string := '"' [^"]* '"' | "'" [^']* "'"
 * unquoted_value := [^ )]+
 * ```
 */
class AspectQueryParser {

    /**
     * Parse a query string into an AST.
     */
    fun parse(query: String): Either<QueryParseError, AspectQueryAST> {
        val tokens = tokenize(query)
        return if (tokens.isEmpty()) {
            QueryParseError.EmptyQuery.left()
        } else {
            val parser = Parser(tokens)
            try {
                val ast = parser.parseQuery()
                if (parser.hasMore()) {
                    QueryParseError.UnexpectedToken(parser.currentToken(), parser.position).left()
                } else {
                    ast.right()
                }
            } catch (e: ParseException) {
                e.error.left()
            }
        }
    }

    private fun tokenize(query: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < query.length) {
            when {
                query[i].isWhitespace() -> {
                    // Skip whitespace
                    i++
                }
                query[i] == '(' -> {
                    tokens.add(Token.LeftParen)
                    i++
                }
                query[i] == ')' -> {
                    tokens.add(Token.RightParen)
                    i++
                }
                query[i] == '"' || query[i] == '\'' -> {
                    // Parse quoted string
                    val quote = query[i]
                    val start = i + 1
                    i++
                    while (i < query.length && query[i] != quote) {
                        i++
                    }
                    if (i >= query.length) {
                        throw ParseException(QueryParseError.UnterminatedString(start))
                    }
                    tokens.add(Token.Value(query.substring(start, i)))
                    i++ // Skip closing quote
                }
                query.startsWith("AND", i) && (i + 3 >= query.length || !query[i + 3].isLetterOrDigit()) -> {
                    tokens.add(Token.And)
                    i += 3
                }
                query.startsWith("OR", i) && (i + 2 >= query.length || !query[i + 2].isLetterOrDigit()) -> {
                    tokens.add(Token.Or)
                    i += 2
                }
                query.startsWith("NOT", i) && (i + 3 >= query.length || !query[i + 3].isLetterOrDigit()) -> {
                    tokens.add(Token.Not)
                    i += 3
                }
                query.startsWith(">=", i) -> {
                    tokens.add(Token.Operator(ComparisonOperator.GREATER_THAN_OR_EQUALS))
                    i += 2
                }
                query.startsWith("<=", i) -> {
                    tokens.add(Token.Operator(ComparisonOperator.LESS_THAN_OR_EQUALS))
                    i += 2
                }
                query.startsWith("!=", i) -> {
                    tokens.add(Token.Operator(ComparisonOperator.NOT_EQUALS))
                    i += 2
                }
                query[i] == '>' -> {
                    tokens.add(Token.Operator(ComparisonOperator.GREATER_THAN))
                    i++
                }
                query[i] == '<' -> {
                    tokens.add(Token.Operator(ComparisonOperator.LESS_THAN))
                    i++
                }
                query[i] == '=' -> {
                    tokens.add(Token.Operator(ComparisonOperator.EQUALS))
                    i++
                }
                query[i].isLetter() -> {
                    // Parse identifier
                    val start = i
                    while (i < query.length && (query[i].isLetterOrDigit() || query[i] == '_')) {
                        i++
                    }
                    tokens.add(Token.Identifier(query.substring(start, i)))
                }
                else -> {
                    // Parse unquoted value until whitespace or special character
                    val start = i
                    while (i < query.length &&
                        !query[i].isWhitespace() &&
                        query[i] != ')' &&
                        query[i] != '(' &&
                        !isOperatorStart(query, i)
                    ) {
                        i++
                    }
                    if (i > start) {
                        tokens.add(Token.Value(query.substring(start, i)))
                    } else {
                        throw ParseException(QueryParseError.UnexpectedCharacter(query[i], i))
                    }
                }
            }
        }

        return tokens
    }

    private fun isOperatorStart(query: String, index: Int): Boolean {
        if (index >= query.length) return false
        return when {
            query.startsWith(">=", index) -> true
            query.startsWith("<=", index) -> true
            query.startsWith("!=", index) -> true
            query[index] == '>' -> true
            query[index] == '<' -> true
            query[index] == '=' -> true
            else -> false
        }
    }

    private sealed class Token {
        data class Identifier(val name: String) : Token()
        data class Operator(val op: ComparisonOperator) : Token()
        data class Value(val value: String) : Token()
        object And : Token()
        object Or : Token()
        object Not : Token()
        object LeftParen : Token()
        object RightParen : Token()
    }

    private class Parser(private val tokens: List<Token>) {
        var position = 0

        fun hasMore(): Boolean = position < tokens.size

        fun currentToken(): Token? = tokens.getOrNull(position)

        fun parseQuery(): AspectQueryAST = parseOrExpression()

        private fun parseOrExpression(): AspectQueryAST {
            var left = parseAndExpression()

            while (hasMore() && currentToken() == Token.Or) {
                position++ // consume OR
                val right = parseAndExpression()
                left = AspectQueryAST.Or(left, right)
            }

            return left
        }

        private fun parseAndExpression(): AspectQueryAST {
            var left = parseNotExpression()

            while (hasMore() && currentToken() == Token.And) {
                position++ // consume AND
                val right = parseNotExpression()
                left = AspectQueryAST.And(left, right)
            }

            return left
        }

        private fun parseNotExpression(): AspectQueryAST = if (hasMore() && currentToken() == Token.Not) {
            position++ // consume NOT
            AspectQueryAST.Not(parseNotExpression())
        } else {
            parsePrimary()
        }

        private fun parsePrimary(): AspectQueryAST = when (val token = currentToken()) {
            is Token.LeftParen -> {
                position++ // consume (
                val expr = parseQuery()
                if (currentToken() != Token.RightParen) {
                    throw ParseException(QueryParseError.MissingClosingParen(position))
                }
                position++ // consume )
                AspectQueryAST.Parentheses(expr)
            }
            is Token.Identifier -> parseComparison()
            else -> throw ParseException(QueryParseError.ExpectedExpression(position))
        }

        private fun parseComparison(): AspectQueryAST {
            val identifier = currentToken() as? Token.Identifier
                ?: throw ParseException(QueryParseError.ExpectedIdentifier(position))
            position++ // consume identifier

            val operator = currentToken() as? Token.Operator
                ?: throw ParseException(QueryParseError.ExpectedOperator(position))
            position++ // consume operator

            val value = when (val token = currentToken()) {
                is Token.Value -> token.value
                is Token.Identifier -> token.name // Allow identifiers as values
                else -> throw ParseException(QueryParseError.ExpectedValue(position))
            }
            position++ // consume value

            return AspectQueryAST.Comparison(identifier.name, operator.op, value)
        }
    }

    private class ParseException(val error: QueryParseError) : Exception()
}

/**
 * Errors that can occur during query parsing.
 */
sealed class QueryParseError {
    object EmptyQuery : QueryParseError()
    data class UnexpectedCharacter(val char: Char, val position: Int) : QueryParseError()
    data class UnterminatedString(val position: Int) : QueryParseError()
    data class UnexpectedToken(val token: Any?, val position: Int) : QueryParseError()
    data class MissingClosingParen(val position: Int) : QueryParseError()
    data class ExpectedExpression(val position: Int) : QueryParseError()
    data class ExpectedIdentifier(val position: Int) : QueryParseError()
    data class ExpectedOperator(val position: Int) : QueryParseError()
    data class ExpectedValue(val position: Int) : QueryParseError()
}
