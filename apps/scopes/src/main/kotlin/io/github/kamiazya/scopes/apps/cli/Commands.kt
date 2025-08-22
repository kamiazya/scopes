package io.github.kamiazya.scopes.apps.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking

/**
 * Update command for modifying existing scopes.
 */
class UpdateCommand :
    CliktCommand(
        name = "update",
        help = "Update an existing scope",
    ) {
    private val id by argument(help = "ID of the scope to update")
    private val title by option("-t", "--title", help = "New title for the scope")
    private val description by option("-d", "--description", help = "New description for the scope")

    override fun run() {
        runBlocking {
            KoinCompositionRoot.scopeCommandAdapter.updateScope(
                id = id,
                title = title,
                description = description,
            ).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                { scope ->
                    echo(KoinCompositionRoot.scopeOutputFormatter.formatUpdateResult(scope))
                },
            )
        }
    }
}

/**
 * Delete command for removing scopes.
 */
class DeleteCommand :
    CliktCommand(
        name = "delete",
        help = "Delete a scope",
    ) {
    private val id by argument(help = "ID of the scope to delete")
    private val cascade by option("--cascade", help = "Delete all child scopes").flag()

    override fun run() {
        runBlocking {
            KoinCompositionRoot.scopeCommandAdapter.deleteScope(id).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                {
                    echo(KoinCompositionRoot.scopeOutputFormatter.formatDeleteResult(id))
                },
            )
        }
    }
}

/**
 * List command for retrieving multiple scopes.
 */
class ListCommand :
    CliktCommand(
        name = "list",
        help = "List scopes",
    ) {
    private val parentId by option("-p", "--parent", help = "Parent scope ID to list children")
    private val root by option("--root", help = "List only root scopes").flag()
    private val offset by option("--offset", help = "Number of items to skip").int().default(0)
    private val limit by option("--limit", help = "Maximum number of items to return").int().default(20)

    override fun run() {
        runBlocking {
            when {
                root -> {
                    KoinCompositionRoot.scopeCommandAdapter.listRootScopes().fold(
                        { error ->
                            echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            echo(KoinCompositionRoot.scopeOutputFormatter.formatScopeList(scopes))
                        },
                    )
                }
                parentId != null -> {
                    KoinCompositionRoot.scopeCommandAdapter.listChildren(parentId!!).fold(
                        { error ->
                            echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            echo(KoinCompositionRoot.scopeOutputFormatter.formatScopeList(scopes))
                        },
                    )
                }
                else -> {
                    // Default: list root scopes
                    KoinCompositionRoot.scopeCommandAdapter.listRootScopes().fold(
                        { error ->
                            echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            echo(KoinCompositionRoot.scopeOutputFormatter.formatScopeList(scopes))
                        },
                    )
                }
            }
        }
    }
}
