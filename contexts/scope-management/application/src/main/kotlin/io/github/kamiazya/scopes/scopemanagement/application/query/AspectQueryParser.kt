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
                        return QueryParseError.UnterminatedString(start).left()
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
                query.startsWith("CONTAINS", i) && (i + 8 >= query.length || !query[i + 8].isLetterOrDigit()) -> {
                    tokens.add(Token.Operator(ComparisonOperator.CONTAINS))
                    i += 8
                }
                query.startsWith("EXISTS", i) && (i + 6 >= query.length || !query[i + 6].isLetterOrDigit()) -> {
                    tokens.add(Token.Operator(ComparisonOperator.EXISTS))
                    i += 6
                }
                query.startsWith("IS_NULL", i) && (i + 7 >= query.length || !query[i + 7].isLetterOrDigit()) -> {
                    tokens.add(Token.Operator(ComparisonOperator.IS_NULL))
                    i += 7
                }
                query.startsWith("IN", i) && (i + 2 >= query.length || !query[i + 2].isLetterOrDigit()) -> {
                    tokens.add(Token.Operator(ComparisonOperator.IN))
                    i += 2
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
                    // Check if the previous token was an IN operator
                    val lastToken = tokens.lastOrNull()
                    val isAfterInOperator = lastToken is Token.Operator && lastToken.op == ComparisonOperator.IN
                    
                    if (isAfterInOperator) {
                        // After IN operator, parse value including commas
                        val start = i
                        while (i < query.length && 
                            !query[i].isWhitespace() && 
                            query[i] != ')' && 
                            query[i] != '(' &&
                            !isLogicalOperatorStart(query, i)) {
                            i++
                        }
                        tokens.add(Token.Value(query.substring(start, i)))
                    } else {
                        // Parse identifier
                        val start = i
                        while (i < query.length && (query[i].isLetterOrDigit() || query[i] == '_')) {
                            i++
                        }
                        tokens.add(Token.Identifier(query.substring(start, i)))
                    }
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
                        return QueryParseError.UnexpectedCharacter(query[i], i).left()
                    }
                }
            }
        }

        return tokens.right()
    }

    private fun isOperatorStart(query: String, index: Int): Boolean {
        if (index >= query.length) return false
        return when {
            query.startsWith("CONTAINS", index) -> true
            query.startsWith("EXISTS", index) -> true
            query.startsWith("IS_NULL", index) -> true
            query.startsWith("IN", index) -> true
            query.startsWith(">=", index) -> true
            query.startsWith("<=", index) -> true
            query.startsWith("!=", index) -> true
            query[index] == '>' -> true
            query[index] == '<' -> true
            query[index] == '=' -> true
            else -> false
        }
    }
    
    private fun isLogicalOperatorStart(query: String, index: Int): Boolean {
        if (index >= query.length) return false
        return when {
            query.startsWith("AND", index) && (index + 3 >= query.length || !query[index + 3].isLetterOrDigit()) -> true
            query.startsWith("OR", index) && (index + 2 >= query.length || !query[index + 2].isLetterOrDigit()) -> true
            query.startsWith("NOT", index) && (index + 3 >= query.length || !query[index + 3].isLetterOrDigit()) -> true
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

            // EXISTS and IS_NULL are unary operators that don't require a value
            val value = when (operator.op) {
                ComparisonOperator.EXISTS, ComparisonOperator.IS_NULL -> ""
                ComparisonOperator.IN -> {
                    // For IN operator, get the comma-separated value and keep it as is
                    when (val token = currentToken()) {
                        is Token.Value -> {
                            position++ // consume value
                            token.value // This will be something like "open,closed"
                        }
                        is Token.Identifier -> {
                            position++ // consume identifier as value
                            token.name 
                        }
                        else -> return QueryParseError.ExpectedValue(position).left()
                    }
                }
                else -> {
                    when (val token = currentToken()) {
                        is Token.Value -> {
                            position++ // consume value
                            token.value
                        }
                        is Token.Identifier -> {
                            position++ // consume identifier as value
                            token.name 
                        }
                        else -> return QueryParseError.ExpectedValue(position).left()
                    }
                }
            }

            return AspectQueryAST.Comparison(identifier.name, operator.op, value).right()
        }
    }
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
