package io.github.kamiazya.scopes.presentation.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.application.usecase.CreateScopeUseCase
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.presentation.cli.commands.CreateCommand

/**
 * Main CLI command for the Scopes application.
 * Entry point for all CLI operations.
 */
class ScopesCommand : CliktCommand(name = "scopes") {
    init {
        // Initialize dependencies
        val scopeRepository = InMemoryScopeRepository()
        val createScopeUseCase = CreateScopeUseCase(scopeRepository)

        // Register subcommands
        subcommands(
            CreateCommand(createScopeUseCase),
        )
    }

    override fun run() {
        // Main command logic - show help if no subcommand
    }
}

