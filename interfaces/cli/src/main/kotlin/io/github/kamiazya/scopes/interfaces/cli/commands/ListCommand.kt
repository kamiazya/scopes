package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.cli.exitcode.ExitCode
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import io.github.kamiazya.scopes.scopemanagement.domain.error.DomainValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.service.AspectManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.service.ValidationService
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * List command for retrieving multiple scopes.
 */
class ListCommand :
    ScopesCliktCommand(
        name = "list",
        help = "List scopes",
    ),
    KoinComponent {
    private val scopeQueryAdapter: ScopeQueryAdapter by inject()
    private val contextQueryAdapter: ContextQueryAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val responseFormatter: ResponseFormatterService = ResponseFormatterService()
    private val aspectService: AspectManagementService = AspectManagementService()
    private val validationService: ValidationService = ValidationService()
    private val debugContext by requireObject<DebugContext>()

    private val parentId by option("-p", "--parent", help = "Parent scope ID to list children")
    private val root by option("--root", help = "List only root scopes").flag()
    private val offset by option("--offset", help = "Number of items to skip").int().default(0)
    private val limit by option("--limit", help = "Maximum number of items to return").int().default(20)
    private val verbose by option("-v", "--verbose", help = "Show all aliases for each scope").flag()
    private val query by option("-q", "--query", help = "Filter by advanced query (e.g., 'priority>=high AND status!=closed')")
    private val ignoreContext by option("--no-context", help = "Ignore the current context and show all scopes").flag()

    // Aspect filtering with completion support
    private val aspects by option(
        "-a",
        "--aspect",
        help = "Filter by aspect: key:value or key=value. First delimiter splits; later ':'/'=' stay in value. Quote values with spaces. Repeatable.",
        completionCandidates = CompletionCandidates.Custom.fromStdout("scopes _complete-aspects"),
    ).multiple()

    override fun run() {
        runBlocking {
            // Validate pagination inputs using ValidationService
            validationService.validatePagination(offset, limit).fold(
                { error ->
                    fail(
                        when (error) {
                            is DomainValidationError.InvalidPagination.OffsetTooSmall ->
                                "Offset must be at least ${error.minOffset}, but was ${error.offset}"
                            is DomainValidationError.InvalidPagination.LimitTooSmall ->
                                "Limit must be at least ${error.minLimit}, but was ${error.limit}"
                            is DomainValidationError.InvalidPagination.LimitTooLarge ->
                                "Limit must not exceed ${error.maxLimit}, but was ${error.limit}"
                        },
                        ExitCode.USAGE_ERROR,
                    )
                },
                { /* valid pagination */ },
            )

            // Parse aspect filters (supports key:value and key=value)
            val aspectFilters = parseAspectFilters(aspects)
            val invalidAspects = aspects.filter { parseAspectEntry(it) == null }
            invalidAspects.forEach { invalid ->
                echo(
                    "Warning: Invalid aspect format: $invalid (expected key:value or key=value)",
                    err = true,
                )
            }

            // Get current context filter if not ignoring context
            val contextFilter = if (!ignoreContext) {
                contextQueryAdapter.getCurrentContext().getOrNull()?.filter
            } else {
                null
            }

            // Combine query with context filter
            val effectiveQuery = when {
                query != null && contextFilter != null -> "($contextFilter) AND ($query)"
                query != null -> query
                contextFilter != null -> contextFilter
                else -> null
            }

            // Show active context if one is being applied
            if (contextFilter != null && !ignoreContext) {
                echo("Using context filter: $contextFilter", err = true)
            }

            when {
                effectiveQuery != null -> {
                    // Use advanced query filtering (either from context, query param, or both)
                    scopeQueryAdapter.listScopesWithQuery(
                        aspectQuery = effectiveQuery,
                        parentId = parentId,
                        offset = offset,
                        limit = limit,
                    ).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { scopes ->
                            val filteredScopes = if (aspectFilters.isNotEmpty()) {
                                echo("Note: aspect filtering is applied after pagination; adjust --offset/--limit to explore more matches.", err = true)
                                filterByAspects(scopes, aspectFilters)
                            } else {
                                scopes
                            }
                            if (verbose) {
                                echo(formatVerboseListWithAliases(filteredScopes))
                            } else {
                                echo(
                                    responseFormatter.formatPagedScopesForCli(
                                        result = io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                                            scopes = filteredScopes,
                                            totalCount = scopes.size,
                                            offset = offset,
                                            limit = limit,
                                        ),
                                        includeDebug = debugContext.debug,
                                        includeAliases = false,
                                    ),
                                )
                            }
                        },
                    )
                }
                root -> {
                    scopeQueryAdapter.listRootScopes(offset, limit).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { page ->
                            val scopes = page.scopes
                            val filteredScopes = if (aspectFilters.isNotEmpty()) {
                                filterByAspects(scopes, aspectFilters)
                            } else {
                                scopes
                            }
                            if (aspectFilters.isNotEmpty()) {
                                echo("Note: aspect filtering is applied after pagination; adjust --offset/--limit to explore more matches.", err = true)
                            }

                            if (verbose) {
                                echo(formatVerboseListWithAliases(filteredScopes))
                            } else {
                                echo(
                                    responseFormatter.formatRootScopesForCli(
                                        result = io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                                            scopes = filteredScopes,
                                            totalCount = filteredScopes.size,
                                            offset = 0,
                                            limit = filteredScopes.size,
                                        ),
                                        includeDebug = debugContext.debug,
                                        includeAliases = false,
                                    ),
                                )
                            }
                        },
                    )
                }
                parentId != null -> {
                    scopeQueryAdapter.listChildren(parentId!!, offset, limit).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { page ->
                            val scopes = page.scopes
                            val filteredScopes = if (aspectFilters.isNotEmpty()) {
                                filterByAspects(scopes, aspectFilters)
                            } else {
                                scopes
                            }
                            if (aspectFilters.isNotEmpty()) {
                                echo("Note: aspect filtering is applied after pagination; adjust --offset/--limit to explore more matches.", err = true)
                            }

                            if (verbose) {
                                echo(formatVerboseListWithAliases(filteredScopes))
                            } else {
                                echo(
                                    responseFormatter.formatPagedScopesForCli(
                                        result = page.copy(scopes = filteredScopes),
                                        includeDebug = debugContext.debug,
                                        includeAliases = false,
                                    ),
                                )
                            }
                        },
                    )
                }
                else -> {
                    // Default: list root scopes
                    scopeQueryAdapter.listRootScopes(offset, limit).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { page ->
                            val scopes = page.scopes
                            val filteredScopes = if (aspectFilters.isNotEmpty()) {
                                filterByAspects(scopes, aspectFilters)
                            } else {
                                scopes
                            }
                            if (aspectFilters.isNotEmpty()) {
                                echo("Note: aspect filtering is applied after pagination; adjust --offset/--limit to explore more matches.", err = true)
                            }

                            if (verbose) {
                                echo(formatVerboseListWithAliases(filteredScopes))
                            } else {
                                echo(
                                    responseFormatter.formatRootScopesForCli(
                                        result = io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                                            scopes = filteredScopes,
                                            totalCount = filteredScopes.size,
                                            offset = 0,
                                            limit = filteredScopes.size,
                                        ),
                                        includeDebug = debugContext.debug,
                                        includeAliases = false,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private suspend fun formatVerboseListWithAliases(scopes: List<ScopeResult>): String {
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
                scopeQueryAdapter.listAliases(scope.id).fold(
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
        aspectService.checkAspectFilters(scope.aspects, aspectFilters)
    }
}
