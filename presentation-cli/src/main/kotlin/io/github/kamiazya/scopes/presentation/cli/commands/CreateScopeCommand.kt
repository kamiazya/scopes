package io.github.kamiazya.scopes.presentation.cli.commands

import arrow.core.fold
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.error.ErrorInfoService
import io.github.kamiazya.scopes.application.error.ErrorMessageFormatter
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import kotlinx.coroutines.runBlocking

/**
 * CLI command for creating scopes using UseCase pattern.
 * Demonstrates clean separation between presentation and application layers.
 */
class CreateScopeCommand(
    private val createScopeHandler: CreateScopeHandler,
    private val errorInfoService: ErrorInfoService,
    private val errorMessageFormatter: ErrorMessageFormatter
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
            ifLeft = { error ->
                val errorInfo = errorInfoService.toErrorInfo(error)
                val errorMessage = errorMessageFormatter.format(errorInfo)
                echo("❌ Error creating scope: $errorMessage", err = true)
                throw com.github.ajalt.clikt.core.ProgramResult(1)
            },
            ifRight = { result: CreateScopeResult ->
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