package io.github.kamiazya.scopes.presentation.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.presentation.cli.utils.toUserMessage
import kotlinx.coroutines.runBlocking

/**
 * CLI command for creating scopes using UseCase pattern.
 * Demonstrates clean separation between presentation and application layers.
 */
class CreateScopeCommand(
    private val createScopeHandler: CreateScopeHandler,
) : CliktCommand(name = "create-scope") {
    
    private val name by option("--name", help = "Scope name").required()
    private val description by option("--description", help = "Scope description")
    private val parent by option("--parent", help = "Parent scope ID")

    override fun run() = runBlocking {
        val command = CreateScope(
            title = name,
            description = description,
            parentId = parent?.let { ScopeId.from(it) },
        )

        createScopeHandler(command).fold(
            ifLeft = { error ->
                echo("❌ Error creating scope: ${error.toUserMessage()}", err = true)
                throw com.github.ajalt.clikt.core.ProgramResult(1)
            },
            ifRight = { result ->
                echo("✅ Created scope: ${result.id}")
                echo("   Title: ${result.title}")
                if (result.description != null) {
                    echo("   Description: ${result.description}")
                }
                if (result.parentId != null) {
                    echo("   Parent: ${result.parentId}")
                }
                echo("   Created at: ${result.createdAt}")
            }
        )
    }
}
