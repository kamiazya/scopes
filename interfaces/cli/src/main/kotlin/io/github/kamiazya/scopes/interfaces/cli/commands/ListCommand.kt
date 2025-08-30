package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * List command for retrieving multiple scopes.
 */
class ListCommand :
    CliktCommand(
        name = "list",
        help = "List scopes",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val debugContext by requireObject<DebugContext>()

    private val parentId by option("-p", "--parent", help = "Parent scope ID to list children")
    private val root by option("--root", help = "List only root scopes").flag()
    private val offset by option("--offset", help = "Number of items to skip").int().default(0)
    private val limit by option("--limit", help = "Maximum number of items to return").int().default(20)
    private val verbose by option("-v", "--verbose", help = "Show all aliases for each scope").flag()
    private val aspect by option("-a", "--aspect", help = "Filter by aspect (format: key=value)")

    override fun run() {
        runBlocking {
            // Parse aspect filter if provided
            val (aspectKey, aspectValue) = if (aspect != null) {
                val parts = aspect!!.split("=", limit = 2)
                if (parts.size != 2) {
                    echo("Error: Invalid aspect format. Use key=value", err = true)
                    return@runBlocking
                }
                parts[0] to parts[1]
            } else {
                null to null
            }

            when {
                aspect != null -> {
                    // Use aspect filtering
                    scopeCommandAdapter.listScopesWithAspect(
                        aspectKey = aspectKey!!,
                        aspectValue = aspectValue!!,
                        parentId = parentId,
                    ).fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            if (verbose) {
                                echo(formatVerboseList(scopes, debugContext))
                            } else {
                                echo(scopeOutputFormatter.formatContractScopeList(scopes, debugContext.debug))
                            }
                        },
                    )
                }
                root -> {
                    scopeCommandAdapter.listRootScopes().fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            if (verbose) {
                                // Fetch aliases for each scope and display verbosely
                                echo(formatVerboseList(scopes, debugContext))
                            } else {
                                echo(scopeOutputFormatter.formatContractScopeList(scopes, debugContext.debug))
                            }
                        },
                    )
                }
                parentId != null -> {
                    scopeCommandAdapter.listChildren(parentId!!).fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            if (verbose) {
                                echo(formatVerboseList(scopes, debugContext))
                            } else {
                                echo(scopeOutputFormatter.formatContractScopeList(scopes, debugContext.debug))
                            }
                        },
                    )
                }
                else -> {
                    // Default: list root scopes
                    scopeCommandAdapter.listRootScopes().fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            if (verbose) {
                                echo(formatVerboseList(scopes, debugContext))
                            } else {
                                echo(scopeOutputFormatter.formatContractScopeList(scopes, debugContext.debug))
                            }
                        },
                    )
                }
            }
        }
    }

    private suspend fun formatVerboseList(scopes: List<ScopeResult>, debugContext: DebugContext): String {
        if (scopes.isEmpty()) {
            return "No scopes found."
        }

        return buildString {
            appendLine("Found ${scopes.size} scope(s):")
            appendLine()

            scopes.forEachIndexed { index, scope ->
                if (index > 0) appendLine()

                // Basic scope info
                appendLine("• ${scope.title}")
                scope.description?.let { appendLine("  Description: $it") }
                if (debugContext.debug) {
                    appendLine("  ID: ${scope.id}")
                }

                // Fetch and display all aliases
                scopeCommandAdapter.listAliases(scope.id).fold(
                    { error ->
                        // If we can't fetch aliases, show just the canonical one
                        if (debugContext.debug) {
                            appendLine("  Aliases: ${scope.canonicalAlias} (ULID: ${scope.id})")
                        } else {
                            appendLine("  Aliases: ${scope.canonicalAlias}")
                        }
                    },
                    { aliasResult ->
                        if (aliasResult.aliases.isEmpty()) {
                            appendLine("  Aliases: None")
                        } else {
                            append("  Aliases: ")
                            val aliasStrings = aliasResult.aliases.map { alias ->
                                val typeLabel = if (alias.isCanonical) {
                                    " (canonical)"
                                } else {
                                    ""
                                }
                                if (debugContext.debug) {
                                    "${alias.aliasName}$typeLabel (ULID: ${scope.id})"
                                } else {
                                    "${alias.aliasName}$typeLabel"
                                }
                            }
                            appendLine(aliasStrings.joinToString(", "))
                        }
                    },
                )

                scope.parentId?.let {
                    if (debugContext.debug) {
                        appendLine("  Parent: $it")
                    }
                }
            }
        }.trim()
    }
}
