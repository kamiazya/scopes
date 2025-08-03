package com.kamiazya.scopes.presentation.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.kamiazya.scopes.application.service.ScopeService
import com.kamiazya.scopes.application.usecase.CreateScopeUseCase
import com.kamiazya.scopes.infrastructure.repository.InMemoryScopeRepository
import com.kamiazya.scopes.presentation.cli.commands.CreateCommand
import com.kamiazya.scopes.presentation.cli.commands.ListCommand

/**
 * Main CLI command for the Scopes application.
 * Entry point for all CLI operations.
 */
class ScopesCommand : CliktCommand(name = "scopes") {
    init {
        // Initialize dependencies
        val scopeRepository = InMemoryScopeRepository()
        val createScopeUseCase = CreateScopeUseCase(scopeRepository)
        val scopeService = ScopeService(scopeRepository, createScopeUseCase)

        // Register subcommands
        subcommands(
            CreateCommand(scopeService),
            ListCommand(scopeService),
        )
    }

    override fun run() {
        // Main command logic - show help if no subcommand
    }
}
