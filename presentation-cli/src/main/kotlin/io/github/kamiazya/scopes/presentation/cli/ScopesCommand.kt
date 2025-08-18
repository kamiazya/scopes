package io.github.kamiazya.scopes.presentation.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.presentation.cli.commands.CreateScopeCommand
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Main CLI command for the Scopes application.
 * Entry point for all CLI operations.
 * Uses Koin dependency injection for Clean Architecture compliance.
 */
class ScopesCommand : CliktCommand(name = "scopes"), KoinComponent {
    private val createScopeCommand: CreateScopeCommand by inject()

    init {
        subcommands(
            createScopeCommand,
        )
    }

    override fun run() {
        // Main command logic - show help if no subcommand
    }
}


