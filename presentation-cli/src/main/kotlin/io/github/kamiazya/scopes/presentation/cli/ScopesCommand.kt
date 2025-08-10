package io.github.kamiazya.scopes.presentation.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.application.error.AppErrorTranslator
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.presentation.cli.commands.CreateScopeCommand
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Main CLI command for the Scopes application.
 * Entry point for all CLI operations.
 * Uses Koin dependency injection for Clean Architecture compliance.
 */
class ScopesCommand : CliktCommand(name = "scopes"), KoinComponent {
    private val createScopeHandler: CreateScopeHandler by inject()
    private val errorTranslator: AppErrorTranslator by inject()

    init {
        subcommands(
            CreateScopeCommand(createScopeHandler, errorTranslator),
        )
    }

    override fun run() {
        // Main command logic - show help if no subcommand
    }
}

