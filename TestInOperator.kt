import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryAST
import io.github.kamiazya.scopes.scopemanagement.application.query.ComparisonOperator

fun main() {
    val parser = AspectQueryParser()
    
    println("Testing IN operator parsing...")
    val queries = listOf(
        "status IN open,closed",
        "status IN open",
        "priority EXISTS",
        "assignee IS_NULL",
        "status CONTAINS pen"
    )
    
    queries.forEach { query ->
        println("\nQuery: '$query'")
        val result = parser.parse(query)
        result.fold(
            { error ->
                println("  ERROR: $error")
            },
            { ast ->
                println("  SUCCESS!")
                when (ast) {
                    is AspectQueryAST.Comparison -> {
                        println("    Key: ${ast.key}")
                        println("    Operator: ${ast.operator}")
                        println("    Value: '${ast.value}'")
                    }
                    else -> println("    AST: $ast")
                }
            }
        )
    }
}