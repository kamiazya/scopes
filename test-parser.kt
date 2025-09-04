import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryParser

fun main() {
    val parser = AspectQueryParser()
    val queries = listOf(
        "status = open",
        "status != closed",
        "status CONTAINS pen",
        "status IN open,closed",
        "priority EXISTS",
        "assignee IS_NULL"
    )
    
    queries.forEach { query ->
        println("Testing: $query")
        val result = parser.parse(query)
        result.fold(
            ifLeft = { error -> println("  ERROR: $error") },
            ifRight = { ast -> println("  SUCCESS: $ast") }
        )
    }
}