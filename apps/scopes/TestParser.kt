fun main() {
    val query = "status IN open,closed"
    println("Query: $query")
    println("Position 3 char: '${query[3]}'")
    println("Character at positions:")
    for (i in 0 until minOf(10, query.length)) {
        println("  $i: '${query[i]}'")
    }
}
