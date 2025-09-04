#!/usr/bin/env kotlin
@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")

// Simplified test to debug IN operator parsing
fun main() {
    val query = "status IN open,closed"
    println("Testing query: $query")
    println("Character at each position:")
    query.forEachIndexed { index, char ->
        println("  Position $index: '$char'")
    }
    
    println("\nChecking what happens at position 7 (where 'IN' starts):")
    println("  startsWith('IN', 7): ${query.startsWith("IN", 7)}")
    println("  Character at position 9: '${query.getOrNull(9)}'")
    println("  Is position 9 letter or digit: ${query.getOrNull(9)?.isLetterOrDigit()}")
    
    // The check in the parser is:
    // query.startsWith("IN", i) && (i + 2 >= query.length || !query[i + 2].isLetterOrDigit())
    val i = 7
    val checkResult = query.startsWith("IN", i) && (i + 2 >= query.length || !query[i + 2].isLetterOrDigit())
    println("\nParser check at position 7 would return: $checkResult")
}