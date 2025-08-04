package io.github.kamiazya.scopes.presentation.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.application.usecase.CreateScopeUseCase
import io.github.kamiazya.scopes.presentation.cli.commands.CreateCommand
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Main CLI command for the Scopes application.
 * Entry point for all CLI operations.
 * Uses Koin dependency injection for Clean Architecture compliance.
 */
class ScopesCommand : CliktCommand(name = "scopes"), KoinComponent {
    private val createScopeUseCase: CreateScopeUseCase by inject()

    init {
        // Register subcommands with injected dependencies
        subcommands(
            CreateCommand(createScopeUseCase),
        )
    }

    override fun run() {
        // Main command logic - show help if no subcommand
    }
}

