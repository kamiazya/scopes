package com.kamiazya.scopes.presentation.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.kamiazya.scopes.application.service.ScopeService
import com.kamiazya.scopes.domain.entity.ScopeId
import kotlinx.coroutines.runBlocking

/**
 * CLI command for listing scopes.
 */
class ListCommand(
    private val scopeService: ScopeService,
) : CliktCommand(name = "list") {
    private val parentId by option("--parent", help = "Show children of this parent scope ID")

    override fun run() =
        runBlocking {
            try {
                val scopes =
                    if (parentId != null) {
                        scopeService.getChildScopes(ScopeId.from(parentId!!))
                    } else {
                        scopeService.getAllScopes()
                    }

                if (scopes.isEmpty()) {
                    echo("No scopes found.")
                    return@runBlocking
                }

                echo("📋 Scopes:")
                scopes.forEach { scope ->
                    echo("  ${scope.id} - ${scope.title}")
                    if (scope.description != null) {
                        echo("    Description: ${scope.description}")
                    }
                    if (scope.parentId != null) {
                        echo("    Parent: ${scope.parentId}")
                    }
                    echo()
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                echo("❌ Error listing scopes: ${e.message}", err = true)
            }
        }
}
