package io.github.kamiazya.scopes.apps.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeById
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
            val command = CreateScope(
                title = title,
                description = description,
                parentId = parentId,
                generateAlias = !noAlias && customAlias == null,
                customAlias = customAlias,
            )

            CompositionRoot.createScopeHandler(command).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                { result ->
                    echo("Scope created successfully!")
                    echo("ID: ${result.id}")
                    echo("Title: ${result.title}")
                    result.description?.let { echo("Description: $it") }
                    result.parentId?.let { echo("Parent ID: $it") }
                    result.canonicalAlias?.let { echo("Alias: $it") }
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
            val query = GetScopeById(id)

            CompositionRoot.getScopeByIdHandler(query).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                { scope ->
                    echo("Scope Details:")
                    echo("ID: ${scope.id}")
                    echo("Title: ${scope.title}")
                    scope.description?.let { echo("Description: $it") }
                    scope.parentId?.let { echo("Parent ID: $it") }
                    echo("Created: ${scope.createdAt}")
                    echo("Updated: ${scope.updatedAt}")
                    if (scope.aspects.isNotEmpty()) {
                        echo("Aspects:")
                        scope.aspects.forEach { (key, values) ->
                            echo("  $key: ${values.joinToString(", ")}")
                        }
                    }
                },
            )
        }
    }
}
