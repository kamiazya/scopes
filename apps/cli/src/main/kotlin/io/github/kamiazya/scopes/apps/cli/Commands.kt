package io.github.kamiazya.scopes.apps.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.kamiazya.scopes.scopemanagement.application.command.DeleteScope
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScope
import io.github.kamiazya.scopes.scopemanagement.application.query.GetChildren
import io.github.kamiazya.scopes.scopemanagement.application.query.GetRootScopes
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
            val command = UpdateScope(
                id = id,
                title = title,
                description = description,
            )

            CompositionRoot.updateScopeHandler(command).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                { scope ->
                    echo("Scope updated successfully!")
                    echo("ID: ${scope.id}")
                    echo("Title: ${scope.title}")
                    scope.description?.let { echo("Description: $it") }
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
            val command = DeleteScope(
                id = id,
                cascade = cascade,
            )

            CompositionRoot.deleteScopeHandler(command).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                {
                    echo("Scope deleted successfully!")
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
                    val query = GetRootScopes(offset = offset, limit = limit)
                    CompositionRoot.getRootScopesHandler(query).fold(
                        { error ->
                            echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            if (scopes.isEmpty()) {
                                echo("No root scopes found")
                            } else {
                                echo("Root Scopes:")
                                scopes.forEach { scope ->
                                    echo("- ${scope.id}: ${scope.title}")
                                    scope.description?.let { echo("  Description: $it") }
                                }
                            }
                        },
                    )
                }
                parentId != null -> {
                    val query = GetChildren(parentId = parentId, offset = offset, limit = limit)
                    CompositionRoot.getChildrenHandler(query).fold(
                        { error ->
                            echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            if (scopes.isEmpty()) {
                                echo("No children found for parent: $parentId")
                            } else {
                                echo("Children of $parentId:")
                                scopes.forEach { scope ->
                                    echo("- ${scope.id}: ${scope.title}")
                                    scope.description?.let { echo("  Description: $it") }
                                }
                            }
                        },
                    )
                }
                else -> {
                    // Default: list root scopes
                    val query = GetRootScopes(offset = offset, limit = limit)
                    CompositionRoot.getRootScopesHandler(query).fold(
                        { error ->
                            echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            if (scopes.isEmpty()) {
                                echo("No scopes found")
                            } else {
                                echo("Scopes:")
                                scopes.forEach { scope ->
                                    echo("- ${scope.id}: ${scope.title}")
                                    scope.description?.let { echo("  Description: $it") }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
