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
            validatePaginationInputs()
            val aspectFilters = parseAndValidateAspectFilters()
            val effectiveQuery = buildEffectiveQuery()

            executeListCommand(aspectFilters, effectiveQuery)
        }
    }

    private fun validatePaginationInputs() {
        validationService.validatePagination(offset, limit).fold(
            { error ->
                val message = when (error) {
                    is DomainValidationError.InvalidPagination.OffsetTooSmall ->
                        "Offset must be at least ${error.minOffset}, but was ${error.offset}"
                    is DomainValidationError.InvalidPagination.LimitTooSmall ->
                        "Limit must be at least ${error.minLimit}, but was ${error.limit}"
                    is DomainValidationError.InvalidPagination.LimitTooLarge ->
                        "Limit must not exceed ${error.maxLimit}, but was ${error.limit}"
                }
                fail(message, ExitCode.USAGE_ERROR)
            },
            { /* valid pagination */ },
        )
    }

    private fun parseAndValidateAspectFilters(): Map<String, List<String>> {
        val aspectFilters = aspectService.parseAspectFilters(aspects)
        val invalidAspects = aspects.filter { aspectService.parseAspectEntry(it) == null }
        invalidAspects.forEach { invalid ->
            echo(
                "Warning: Invalid aspect format: $invalid (expected key:value or key=value)",
                err = true,
            )
        }
        return aspectFilters
    }

    private suspend fun buildEffectiveQuery(): String? {
        val contextFilter = if (!ignoreContext) {
            contextQueryAdapter.getCurrentContext().getOrNull()?.filter
        } else {
            null
        }

        if (contextFilter != null && !ignoreContext) {
            echo("Using context filter: $contextFilter", err = true)
        }

        return when {
            query != null && contextFilter != null -> "($contextFilter) AND ($query)"
            query != null -> query
            contextFilter != null -> contextFilter
            else -> null
        }
    }

    private suspend fun executeListCommand(aspectFilters: Map<String, List<String>>, effectiveQuery: String?) {
        when {
            effectiveQuery != null -> handleQueryBasedListing(aspectFilters, effectiveQuery)
            root -> handleRootScopeListing(aspectFilters)
            parentId != null -> handleChildScopeListing(aspectFilters)
            else -> handleDefaultListing(aspectFilters)
        }
    }

    private suspend fun handleQueryBasedListing(aspectFilters: Map<String, List<String>>, effectiveQuery: String) {
        scopeQueryAdapter.listScopesWithQuery(
            aspectQuery = effectiveQuery,
            parentId = parentId,
            offset = offset,
            limit = limit,
        ).fold(
            { error -> handleContractError(error) },
            { scopes -> displayFilteredScopes(scopes, aspectFilters) },
        )
    }

    private suspend fun handleRootScopeListing(aspectFilters: Map<String, List<String>>) {
        scopeQueryAdapter.listRootScopes(offset, limit).fold(
            { error -> handleContractError(error) },
            { page -> displayPagedScopes(page.scopes, aspectFilters, true) },
        )
    }

    private suspend fun handleChildScopeListing(aspectFilters: Map<String, List<String>>) {
        scopeQueryAdapter.listChildren(parentId!!, offset, limit).fold(
            { error -> handleContractError(error) },
            { page -> displayPagedScopesFromResult(page, aspectFilters) },
        )
    }

    private suspend fun handleDefaultListing(aspectFilters: Map<String, List<String>>) {
        scopeQueryAdapter.listRootScopes(offset, limit).fold(
            { error -> handleContractError(error) },
            { page -> displayPagedScopes(page.scopes, aspectFilters, true) },
        )
    }

    private suspend fun displayFilteredScopes(scopes: List<ScopeResult>, aspectFilters: Map<String, List<String>>) {
        val filteredScopes = applyAspectFilters(scopes, aspectFilters)
        displayScopesOutput(filteredScopes) {
            responseFormatter.formatPagedScopesForCli(
                result = io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                    scopes = filteredScopes,
                    totalCount = scopes.size,
                    offset = offset,
                    limit = limit,
                ),
                includeDebug = debugContext.debug,
                includeAliases = false,
            )
        }
    }

    private suspend fun displayPagedScopes(scopes: List<ScopeResult>, aspectFilters: Map<String, List<String>>, isRoot: Boolean) {
        val filteredScopes = applyAspectFilters(scopes, aspectFilters)
        displayScopesOutput(filteredScopes) {
            if (isRoot) {
                responseFormatter.formatRootScopesForCli(
                    result = io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                        scopes = filteredScopes,
                        totalCount = filteredScopes.size,
                        offset = 0,
                        limit = filteredScopes.size,
                    ),
                    includeDebug = debugContext.debug,
                    includeAliases = false,
                )
            } else {
                responseFormatter.formatPagedScopesForCli(
                    result = io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                        scopes = filteredScopes,
                        totalCount = filteredScopes.size,
                        offset = offset,
                        limit = limit,
                    ),
                    includeDebug = debugContext.debug,
                    includeAliases = false,
                )
            }
        }
    }

    private suspend fun displayPagedScopesFromResult(
        page: io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult,
        aspectFilters: Map<String, List<String>>,
    ) {
        val filteredScopes = applyAspectFilters(page.scopes, aspectFilters)
        displayScopesOutput(filteredScopes) {
            responseFormatter.formatPagedScopesForCli(
                result = page.copy(scopes = filteredScopes),
                includeDebug = debugContext.debug,
                includeAliases = false,
            )
        }
    }

    private fun applyAspectFilters(scopes: List<ScopeResult>, aspectFilters: Map<String, List<String>>): List<ScopeResult> = if (aspectFilters.isNotEmpty()) {
        echo("Note: aspect filtering is applied after pagination; adjust --offset/--limit to explore more matches.", err = true)
        filterByAspects(scopes, aspectFilters)
    } else {
        scopes
    }

    private suspend inline fun displayScopesOutput(scopes: List<ScopeResult>, formatNonVerbose: () -> String) {
        if (verbose) {
            echo(formatVerboseListWithAliases(scopes))
        } else {
            echo(formatNonVerbose())
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
                appendScopeInfo(scope)
            }
        }.trim()
    }

    private suspend fun StringBuilder.appendScopeInfo(scope: ScopeResult) {
        appendBasicScopeInfo(scope)
        appendScopeAliases(scope)
        appendParentInfo(scope)
    }

    private fun StringBuilder.appendBasicScopeInfo(scope: ScopeResult) {
        appendLine("• ${scope.title}")
        scope.description?.let { appendLine("  Description: $it") }
        if (debugContext.debug) {
            appendLine("  ID: ${scope.id}")
        }
    }

    private suspend fun StringBuilder.appendScopeAliases(scope: ScopeResult) {
        scopeQueryAdapter.listAliases(scope.id).fold(
            { error ->
                appendFallbackAliasInfo(scope)
            },
            { aliasResult ->
                appendAliasListInfo(scope, aliasResult)
            },
        )
    }

    private fun StringBuilder.appendFallbackAliasInfo(scope: ScopeResult) {
        if (debugContext.debug) {
            appendLine("  Aliases: ${scope.canonicalAlias} (ULID: ${scope.id})")
        } else {
            appendLine("  Aliases: ${scope.canonicalAlias}")
        }
    }

    private fun StringBuilder.appendAliasListInfo(
        scope: ScopeResult,
        aliasResult: io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult,
    ) {
        if (aliasResult.aliases.isEmpty()) {
            appendLine("  Aliases: None")
        } else {
            append("  Aliases: ")
            val aliasStrings = formatAliasStrings(scope, aliasResult.aliases)
            appendLine(aliasStrings.joinToString(", "))
        }
    }

    private fun formatAliasStrings(scope: ScopeResult, aliases: List<io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo>): List<String> =
        aliases.map { alias ->
            val typeLabel = if (alias.isCanonical) " (canonical)" else ""
            if (debugContext.debug) {
                "${alias.aliasName}$typeLabel (ULID: ${scope.id})"
            } else {
                "${alias.aliasName}$typeLabel"
            }
        }

    private fun StringBuilder.appendParentInfo(scope: ScopeResult) {
        scope.parentId?.let {
            if (debugContext.debug) {
                appendLine("  Parent: $it")
            }
        }
    }

    /**
     * Filters scopes by their aspects.
     * A scope matches if it has all the specified aspect key:value pairs.
     */
    private fun filterByAspects(scopes: List<ScopeResult>, aspectFilters: Map<String, List<String>>): List<ScopeResult> = scopes.filter { scope ->
        aspectService.checkAspectFilters(scope.aspects, aspectFilters)
    }
}
