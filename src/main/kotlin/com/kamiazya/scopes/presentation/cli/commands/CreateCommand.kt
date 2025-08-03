package com.kamiazya.scopes.presentation.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.kamiazya.scopes.application.service.ScopeService
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.usecase.CreateScopeRequest
import kotlinx.coroutines.runBlocking

/**
 * CLI command for creating new scopes.
 */
class CreateCommand(
    private val scopeService: ScopeService,
) : CliktCommand(name = "create") {
    private val title by option("-t", "--title", help = "Scope title").required()
    private val description by option("-d", "--description", help = "Scope description")
    private val parentId by option("--parent", help = "Parent scope ID")

    override fun run() =
        runBlocking {
            try {
                val request =
                    CreateScopeRequest(
                        title = title,
                        description = description,
                        parentId = parentId?.let { ScopeId.from(it) },
                    )

                val response = scopeService.createScope(request)
                echo("✅ Created scope: ${response.scope.id}")
                echo("   Title: ${response.scope.title}")
                if (response.scope.description != null) {
                    echo("   Description: ${response.scope.description}")
                }
                if (response.scope.parentId != null) {
                    echo("   Parent: ${response.scope.parentId}")
                }
            } catch (e: Exception) {
                echo("❌ Error creating scope: ${e.message}", err = true)
            }
        }
}
