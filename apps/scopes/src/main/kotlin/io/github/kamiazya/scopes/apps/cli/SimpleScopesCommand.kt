package io.github.kamiazya.scopes.apps.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking

/**
 * Main CLI command for Scopes application.
 */
class SimpleScopesCommand :
    CliktCommand(
        name = "scopes",
        help = "Scopes - AI-Native Task Management System",
    ) {
    init {
        // Initialize Koin DI
        KoinCompositionRoot.init()

        subcommands(
            CreateCommand(),
            GetCommand(),
            UpdateCommand(),
            DeleteCommand(),
            ListCommand(),
        )
    }

    override fun run() {
        // Nothing to do here - subcommands will be executed
    }
}

/**
 * Create command for creating new scopes.
 */
class CreateCommand :
    CliktCommand(
        name = "create",
        help = "Create a new scope",
    ) {
    private val title by argument(help = "Title of the scope")
    private val description by option("-d", "--description", help = "Description of the scope")
    private val parentId by option("-p", "--parent", help = "Parent scope ID")
    private val noAlias by option("--no-alias", help = "Don't generate an alias").flag()
    private val customAlias by option("-a", "--alias", help = "Custom alias for the scope")

    override fun run() {
        runBlocking {
            KoinCompositionRoot.scopeCommandAdapter.createScope(
                title = title,
                description = description,
                parentId = parentId,
                generateAlias = !noAlias && customAlias == null,
                customAlias = customAlias,
            ).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                { result ->
                    echo(KoinCompositionRoot.scopeOutputFormatter.formatCreateResult(result))
                },
            )
        }
    }
}

/**
 * Get command for retrieving scopes.
 */
class GetCommand :
    CliktCommand(
        name = "get",
        help = "Get a scope by ID",
    ) {
    private val id by argument(help = "ID of the scope to retrieve")

    override fun run() {
        runBlocking {
            KoinCompositionRoot.scopeCommandAdapter.getScopeById(id).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                { scope ->
                    echo(KoinCompositionRoot.scopeOutputFormatter.formatScope(scope))
                },
            )
        }
    }
}
