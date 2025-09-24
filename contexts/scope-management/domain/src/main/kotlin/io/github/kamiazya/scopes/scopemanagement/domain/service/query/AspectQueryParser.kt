package io.github.kamiazya.scopes.scopemanagement.domain.service.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.QueryParseError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.query.AspectQueryAST
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.query.ComparisonOperator

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
    fun parse(query: String): Either<QueryParseError, AspectQueryAST> = try {
        val tokensResult = tokenizeEither(query)
        tokensResult.fold(
            ifLeft = { it.left() },
            ifRight = { tokens ->
                if (tokens.isEmpty()) {
                    QueryParseError.EmptyQuery.left()
                } else {
                    val parser = Parser(tokens)
                    val parseResult = parser.parseQuery()
                    parseResult.fold(
                        ifLeft = { it.left() },
                        ifRight = { ast ->
                            if (parser.hasMore()) {
                                QueryParseError.UnexpectedToken(parser.currentToken(), parser.position).left()
                            } else {
                                ast.right()
                            }
                        },
                    )
                }
            },
        )
    } catch (e: Exception) {
        QueryParseError.EmptyQuery.left() // Fallback for any unexpected errors
    }

    private fun tokenizeEither(query: String): Either<QueryParseError, List<Token>> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < query.length) {
            val result = processNextToken(query, i, tokens)
            when (result) {
                is TokenizeResult.Success -> i = result.newPosition
                is TokenizeResult.Error -> return result.error.left()
            }
        }

        return tokens.right()
    }

    private fun processNextToken(query: String, position: Int, tokens: MutableList<Token>): TokenizeResult = when {
        query[position].isWhitespace() -> processWhitespace(position)
        query[position] == '(' -> processLeftParen(position, tokens)
        query[position] == ')' -> processRightParen(position, tokens)
        query[position] == '"' || query[position] == '\'' -> processQuotedString(query, position, tokens)
        isLogicalOperator(query, position) -> processLogicalOperator(query, position, tokens)
        isComparisonOperator(query, position) -> processComparisonOperator(query, position, tokens)
        query[position].isLetter() -> processIdentifier(query, position, tokens)
        else -> processUnquotedValue(query, position, tokens)
    }

    private fun processWhitespace(position: Int): TokenizeResult.Success = TokenizeResult.Success(position + 1)

    private fun processLeftParen(position: Int, tokens: MutableList<Token>): TokenizeResult.Success {
        tokens.add(Token.LeftParen)
        return TokenizeResult.Success(position + 1)
    }

    private fun processRightParen(position: Int, tokens: MutableList<Token>): TokenizeResult.Success {
        tokens.add(Token.RightParen)
        return TokenizeResult.Success(position + 1)
    }

    private fun processQuotedString(query: String, position: Int, tokens: MutableList<Token>): TokenizeResult {
        val quote = query[position]
        val start = position + 1
        var i = position + 1

        while (i < query.length && query[i] != quote) {
            i++
        }

        return if (i >= query.length) {
            TokenizeResult.Error(QueryParseError.UnterminatedString(start))
        } else {
            tokens.add(Token.Value(query.substring(start, i)))
            TokenizeResult.Success(i + 1) // Skip closing quote
        }
    }

    private fun isLogicalOperator(query: String, position: Int): Boolean = (query.startsWith("AND", position) && isWordBoundary(query, position, 3)) ||
        (query.startsWith("OR", position) && isWordBoundary(query, position, 2)) ||
        (query.startsWith("NOT", position) && isWordBoundary(query, position, 3))

    private fun processLogicalOperator(query: String, position: Int, tokens: MutableList<Token>): TokenizeResult.Success = when {
        query.startsWith("AND", position) -> {
            tokens.add(Token.And)
            TokenizeResult.Success(position + 3)
        }
        query.startsWith("OR", position) -> {
            tokens.add(Token.Or)
            TokenizeResult.Success(position + 2)
        }
        query.startsWith("NOT", position) -> {
            tokens.add(Token.Not)
            TokenizeResult.Success(position + 3)
        }
        else -> error("Unexpected logical operator") // Should never happen due to isLogicalOperator check
    }

    private fun isComparisonOperator(query: String, position: Int): Boolean = query.startsWith(">=", position) ||
        query.startsWith("<=", position) ||
        query.startsWith("!=", position) ||
        query[position] == '>' ||
        query[position] == '<' ||
        query[position] == '='

    private fun processComparisonOperator(query: String, position: Int, tokens: MutableList<Token>): TokenizeResult.Success = when {
        query.startsWith(">=", position) -> {
            tokens.add(Token.Operator(ComparisonOperator.GREATER_THAN_OR_EQUALS))
            TokenizeResult.Success(position + 2)
        }
        query.startsWith("<=", position) -> {
            tokens.add(Token.Operator(ComparisonOperator.LESS_THAN_OR_EQUALS))
            TokenizeResult.Success(position + 2)
        }
        query.startsWith("!=", position) -> {
            tokens.add(Token.Operator(ComparisonOperator.NOT_EQUALS))
            TokenizeResult.Success(position + 2)
        }
        query[position] == '>' -> {
            tokens.add(Token.Operator(ComparisonOperator.GREATER_THAN))
            TokenizeResult.Success(position + 1)
        }
        query[position] == '<' -> {
            tokens.add(Token.Operator(ComparisonOperator.LESS_THAN))
            TokenizeResult.Success(position + 1)
        }
        query[position] == '=' -> {
            tokens.add(Token.Operator(ComparisonOperator.EQUALS))
            TokenizeResult.Success(position + 1)
        }
        else -> error("Unexpected comparison operator") // Should never happen due to isComparisonOperator check
    }

    private fun processIdentifier(query: String, position: Int, tokens: MutableList<Token>): TokenizeResult.Success {
        val start = position
        var i = position
        while (i < query.length && (query[i].isLetterOrDigit() || query[i] == '_')) {
            i++
        }
        tokens.add(Token.Identifier(query.substring(start, i)))
        return TokenizeResult.Success(i)
    }

    private fun processUnquotedValue(query: String, position: Int, tokens: MutableList<Token>): TokenizeResult {
        val start = position
        var i = position

        while (i < query.length &&
            !query[i].isWhitespace() &&
            query[i] != ')' &&
            query[i] != '(' &&
            !isOperatorStart(query, i)
        ) {
            i++
        }

        return if (i > start) {
            tokens.add(Token.Value(query.substring(start, i)))
            TokenizeResult.Success(i)
        } else {
            TokenizeResult.Error(QueryParseError.UnexpectedCharacter(query[i], i))
        }
    }

    private fun isWordBoundary(query: String, position: Int, length: Int): Boolean =
        position + length >= query.length || !query[position + length].isLetterOrDigit()

    private sealed class TokenizeResult {
        data class Success(val newPosition: Int) : TokenizeResult()
        data class Error(val error: QueryParseError) : TokenizeResult()
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

        fun parseQuery(): Either<QueryParseError, AspectQueryAST> = parseOrExpression()

        private fun parseOrExpression(): Either<QueryParseError, AspectQueryAST> {
            return parseAndExpression().fold(
                ifLeft = { it.left() },
                ifRight = { left ->
                    var result = left
                    while (hasMore() && currentToken() == Token.Or) {
                        position++ // consume OR
                        parseAndExpression().fold(
                            ifLeft = { return it.left() },
                            ifRight = { right ->
                                result = AspectQueryAST.Or(result, right)
                            },
                        )
                    }
                    result.right()
                },
            )
        }

        private fun parseAndExpression(): Either<QueryParseError, AspectQueryAST> {
            return parseNotExpression().fold(
                ifLeft = { it.left() },
                ifRight = { left ->
                    var result = left
                    while (hasMore() && currentToken() == Token.And) {
                        position++ // consume AND
                        parseNotExpression().fold(
                            ifLeft = { return it.left() },
                            ifRight = { right ->
                                result = AspectQueryAST.And(result, right)
                            },
                        )
                    }
                    result.right()
                },
            )
        }

        private fun parseNotExpression(): Either<QueryParseError, AspectQueryAST> = if (hasMore() && currentToken() == Token.Not) {
            position++ // consume NOT
            parseNotExpression().fold(
                ifLeft = { it.left() },
                ifRight = { expr -> AspectQueryAST.Not(expr).right() },
            )
        } else {
            parsePrimary()
        }

        private fun parsePrimary(): Either<QueryParseError, AspectQueryAST> = when (val token = currentToken()) {
            is Token.LeftParen -> {
                position++ // consume (
                parseQuery().fold(
                    ifLeft = { it.left() },
                    ifRight = { expr ->
                        if (currentToken() != Token.RightParen) {
                            QueryParseError.MissingClosingParen(position).left()
                        } else {
                            position++ // consume )
                            AspectQueryAST.Parentheses(expr).right()
                        }
                    },
                )
            }
            is Token.Identifier -> parseComparison()
            else -> QueryParseError.ExpectedExpression(position).left()
        }

        private fun parseComparison(): Either<QueryParseError, AspectQueryAST> {
            val identifier = currentToken() as? Token.Identifier
                ?: return QueryParseError.ExpectedIdentifier(position).left()
            position++ // consume identifier

            val operator = currentToken() as? Token.Operator
                ?: return QueryParseError.ExpectedOperator(position).left()
            position++ // consume operator

            val value = when (val token = currentToken()) {
                is Token.Value -> token.value
                is Token.Identifier -> token.name // Allow identifiers as values
                else -> return QueryParseError.ExpectedValue(position).left()
            }
            position++ // consume value

            return AspectQueryAST.Comparison(identifier.name, operator.op, value).right()
        }
    }
}
