package io.github.kamiazya.scopes.presentation.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.AppErrorTranslator
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.application.usecase.fold
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.presentation.cli.utils.toUserMessage
import kotlinx.coroutines.runBlocking

/**
 * CLI command for creating scopes using UseCase pattern.
 * Demonstrates clean separation between presentation and application layers.
 */
class CreateScopeCommand(
    private val createScopeHandler: CreateScopeHandler,
    private val errorTranslator: AppErrorTranslator,
) : CliktCommand(name = "create") {
    
    private val name by option("--name", help = "Scope name").required()
    private val description by option("--description", help = "Scope description")
    private val parent by option("--parent", help = "Parent scope ID")

    override fun run() = runBlocking {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            echo("❌ Error: Scope name cannot be empty or contain only whitespace", err = true)
            throw com.github.ajalt.clikt.core.ProgramResult(1)
        }
        
        val command = CreateScope(
            title = trimmedName,
            description = description,
            parentId = parent,
        )

        createScopeHandler(command).fold(
            ifOk = { result: CreateScopeResult ->
                echo("✅ Created scope: ${result.id}")
                echo("   Title: ${result.title}")
                if (result.description != null) {
                    echo("   Description: ${result.description}")
                }
                if (result.parentId != null) {
                    echo("   Parent: ${result.parentId}")
                }
                echo("   Created at: ${result.createdAt}")
            },
            ifErr = { error: ApplicationError ->
                echo("❌ Error creating scope: ${error.toUserMessage(errorTranslator)}", err = true)
                throw com.github.ajalt.clikt.core.ProgramResult(1)
            }
        )
    }
}
