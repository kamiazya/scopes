#!/usr/bin/env kotlin

@file:Import("contexts/scope-management/application/src/main/kotlin/io/github/kamiazya/scopes/scopemanagement/application/query/AspectQueryParser.kt")

import io.github.kamiazya.scopes.scopemanagement.application.query.*

fun main() {
    val parser = AspectQueryParser()
    
    println("Testing IN operator with spaces...")
    val result = parser.parse("status IN open, closed, pending")
    
    when {
        result.isLeft() -> {
            println("ERROR: ${result.leftOrNull()}")
        }
        result.isRight() -> {
            val ast = result.getOrNull()!!
            println("SUCCESS: $ast")
            
            if (ast is AspectQueryAST.Comparison) {
                println("Field: ${ast.field}")
                println("Operator: ${ast.operator}")
                println("Value: '${ast.value}'")
                println("Expected: 'open, closed, pending'")
                println("Matches: ${ast.value == "open, closed, pending"}")
            }
        }
    }
}