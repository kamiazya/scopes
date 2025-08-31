package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
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

    // Aspect filtering with completion support
    private val aspects by option(
        "-a",
        "--aspect",
        help = "Filter by aspect (format: key:value). Can be specified multiple times.",
        completionCandidates = CompletionCandidates.Custom.fromStdout("scopes _complete-aspects"),
    ).multiple()

    override fun run() {
        runBlocking {
            // Parse aspect filters
            val aspectFilters = aspects.mapNotNull { aspectStr ->
                val parts = aspectStr.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    if (key.isEmpty() || value.isEmpty()) {
                        echo("Warning: Invalid aspect format: $aspectStr (expected non-empty key:value)", err = true)
                        null
                    } else {
                        key to value
                    }
                } else {
                    echo("Warning: Invalid aspect format: $aspectStr (expected key:value)", err = true)
                    null
                }
            }.groupBy({ it.first }, { it.second })

            when {
                root -> {
                    scopeCommandAdapter.listRootScopes().fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            val filteredScopes = if (aspectFilters.isNotEmpty()) {
                                filterByAspects(scopes, aspectFilters)
                            } else {
                                scopes
                            }

                            if (verbose) {
                                // Fetch aliases for each scope and display verbosely
                                echo(formatVerboseList(filteredScopes, debugContext))
                            } else {
                                echo(scopeOutputFormatter.formatContractScopeList(filteredScopes, debugContext.debug))
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
                            val filteredScopes = if (aspectFilters.isNotEmpty()) {
                                filterByAspects(scopes, aspectFilters)
                            } else {
                                scopes
                            }

                            if (verbose) {
                                echo(formatVerboseList(filteredScopes, debugContext))
                            } else {
                                echo(scopeOutputFormatter.formatContractScopeList(filteredScopes, debugContext.debug))
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
                            val filteredScopes = if (aspectFilters.isNotEmpty()) {
                                filterByAspects(scopes, aspectFilters)
                            } else {
                                scopes
                            }

                            if (verbose) {
                                echo(formatVerboseList(filteredScopes, debugContext))
                            } else {
                                echo(scopeOutputFormatter.formatContractScopeList(filteredScopes, debugContext.debug))
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

    /**
     * Filters scopes by their aspects.
     * A scope matches if it has all the specified aspect key:value pairs.
     */
    private fun filterByAspects(scopes: List<ScopeResult>, aspectFilters: Map<String, List<String>>): List<ScopeResult> = scopes.filter { scope ->
        aspectFilters.all { (key, requiredValues) ->
            val scopeValues = scope.aspects[key] ?: emptyList()
            requiredValues.all { requiredValue ->
                scopeValues.contains(requiredValue)
            }
        }
    }
}
