package io.github.kamiazya.scopes.interfaces.mcp.completions

import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.mcp.server.ServerRegistrar
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.modelcontextprotocol.kotlin.sdk.CompleteRequest
import io.modelcontextprotocol.kotlin.sdk.CompleteResult
import io.modelcontextprotocol.kotlin.sdk.Method
import io.modelcontextprotocol.kotlin.sdk.PromptReference
import io.modelcontextprotocol.kotlin.sdk.ResourceTemplateReference
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking

/**
 * Registrar for MCP completion provider.
 *
 * Provides minimal completions for:
 * - Prompt arguments: alias/timeHorizon on prompts.scopes.*
 */
class CompletionRegistrar(private val contextFactory: () -> Pair<Ports, Services>) : ServerRegistrar {

    override fun register(server: Server) {
        // Register completion/complete handler
        server.setRequestHandler<CompleteRequest>(Method.Defined.CompletionComplete) { req, _ ->
            val (ports, services) = contextFactory()
            runBlocking {
                handleComplete(req, ports, services)
            }
        }
    }

    private suspend fun handleComplete(req: CompleteRequest, ports: Ports, services: Services): CompleteResult = when (val ref = req.ref) {
        is PromptReference -> completePromptArgument(ref, req.argument, ports, services)
        is ResourceTemplateReference -> completeResourceTemplateArgument(ref, req.argument, ports, services)
        else -> CompleteResult(CompleteResult.Completion(values = emptyList(), total = 0, hasMore = false))
    }

}

private suspend fun completePromptArgument(ref: PromptReference, arg: CompleteRequest.Argument, ports: Ports, services: Services): CompleteResult {
        val name = ref.name
        val query = arg.value.trim()

        // Support our prompts
        val isScopesPrompt = name == "prompts.scopes.plan" || name == "prompts.scopes.summarize" || name == "prompts.scopes.outline"

        if (!isScopesPrompt) {
            return CompleteResult(CompleteResult.Completion(values = emptyList(), total = 0, hasMore = false))
        }

        return when (arg.name) {
            "alias" -> aliasCompletions(query, ports, services)
            "timeHorizon" -> horizonCompletions(query)
            else -> CompleteResult(CompleteResult.Completion(values = emptyList(), total = 0, hasMore = false))
        }
}

private suspend fun aliasCompletions(prefix: String, ports: Ports, services: Services, canonicalOnly: Boolean = false): CompleteResult {
        // Strategy: BFS from roots up to a safe limit (depth and node count),
        // accumulate canonicalAlias; prefer prefix matches, fallback to contains.
        val roots = ports.query.getRootScopes(GetRootScopesQuery(limit = 200))
        val visited = mutableSetOf<String>()
        val aliases = mutableListOf<String>()
        val titles = mutableListOf<Pair<String, String>>() // (canonicalAlias, title)
        val byCanonical = mutableMapOf<String, String>() // canonicalAlias -> scopeId

        fun addScope(s: ScopeResult) {
            aliases += s.canonicalAlias
            titles += s.canonicalAlias to s.title
            byCanonical[s.canonicalAlias] = s.id
        }

        val queue: ArrayDeque<Pair<ScopeResult, Int>> = ArrayDeque()
        roots.fold({ emptyList<ScopeResult>() }, { it.scopes }).forEach { root ->
            if (visited.add(root.id)) {
                addScope(root)
                queue.add(root to 1)
            }
        }

        val maxNodes = 3000
        val maxDepth = 6
        var nodes = queue.size
        while (queue.isNotEmpty() && nodes < maxNodes) {
            val (node, depth) = queue.removeFirst()
            if (depth >= maxDepth) continue
            val children = ports.query.getChildren(GetChildrenQuery(parentId = node.id)).fold({ emptyList<ScopeResult>() }, { it.scopes })
            for (c in children) {
                if (visited.add(c.id)) {
                    addScope(c)
                    nodes++
                    if (nodes >= maxNodes) break
                    queue.add(c to (depth + 1))
                }
            }
        }

        val q = prefix.lowercase()
        val distinct = aliases.distinct()

        // Match by canonical alias (prefix -> contains), then by title, then (optionally) by alternate aliases
        val prefixMatches = distinct.filter { q.isEmpty() || it.lowercase().startsWith(q) }

        val titleMatches = titles.asSequence()
            .filter { (_, t) -> q.isNotEmpty() && t.lowercase().startsWith(q) }
            .map { (a, _) -> a }
            .filter { it !in prefixMatches }
            .distinct()
            .toList()

        val containsMatches = distinct.asSequence()
            .filter { it !in prefixMatches && it !in titleMatches }
            .filter { q.isNotEmpty() && it.lowercase().contains(q) }
            .toList()

        val maxAliasLookups = 300
        var looked = 0
        val altAliasDirect = mutableListOf<String>()
        val altAliasCanonical = mutableListOf<String>()
        if (q.isNotEmpty()) {
            // Iterate scopes prioritizing those not already matched by canonical/title
            val remainingCanonicals = titles.asSequence()
                .map { (a, _) -> a }
                .filter { it !in prefixMatches && it !in titleMatches && it !in containsMatches }
                .toList()
            for (canonical in remainingCanonicals) {
                if (looked >= maxAliasLookups) break
                val id = byCanonical[canonical] ?: continue
                val res = ports.query.listAliases(ListAliasesQuery(scopeId = id))
                looked++
                val alts = res.fold({ emptyList<AliasInfo>() }, { it.aliases })
                val matches = alts.asSequence()
                    .map { it.aliasName }
                    .filter { it.lowercase().startsWith(q) || it.lowercase().contains(q) }
                    .toList()
                if (matches.isNotEmpty()) {
                    if (!canonicalOnly) {
                        altAliasDirect += matches
                    } else {
                        altAliasCanonical += canonical
                    }
                }
                if (altAliasDirect.size + altAliasCanonical.size >= 100) break
            }
        }

        // Compose results with priority and cap at 100
        val ordered = buildList {
            addAll(prefixMatches)
            addAll(titleMatches)
            addAll(containsMatches)
        }
            .distinct()

        val combined = if (!canonicalOnly) (ordered + altAliasDirect).distinct() else (ordered + altAliasCanonical).distinct()
        val results = combined.take(100)

        services.logger.debug("Completions(alias): query='$prefix' nodes=$nodes depth<=$maxDepth -> ${results.size} values")
        return CompleteResult(
            completion = CompleteResult.Completion(values = results, total = results.size, hasMore = false),
        )
}

private fun horizonCompletions(prefix: String): CompleteResult {
        val base = listOf("1 week", "2 weeks", "1 month", "3 months")
        val q = prefix.lowercase()
        val filtered = base.filter { it.lowercase().startsWith(q) || q.isEmpty() }
        return CompleteResult(CompleteResult.Completion(values = filtered, total = filtered.size, hasMore = false))
}

private suspend fun CompletionRegistrar.completeResourceTemplateArgument(
    ref: ResourceTemplateReference,
    arg: CompleteRequest.Argument,
    ports: Ports,
    services: Services,
): CompleteResult {
    val template = ref.uri
    return when {
        // Our templates that include a canonicalAlias placeholder
        template.startsWith("scopes:/tree/") || template.startsWith("scopes:/tree.md/") || template.startsWith("scopes:/scope/") -> {
            when (arg.name) {
                "canonicalAlias" -> aliasCompletions(arg.value, ports, services, canonicalOnly = true)
                "alias" -> aliasCompletions(arg.value, ports, services, canonicalOnly = false)
                "depth" -> depthCompletions(arg.value)
                else -> CompleteResult(CompleteResult.Completion(values = emptyList(), total = 0, hasMore = false))
            }
        }
        else -> CompleteResult(CompleteResult.Completion(values = emptyList(), total = 0, hasMore = false))
    }
}

private fun depthCompletions(prefix: String): CompleteResult {
    val base = listOf("1", "2", "3", "4", "5")
    val q = prefix.lowercase()
    val filtered = base.filter { it.startsWith(q) || q.isEmpty() }
    return CompleteResult(CompleteResult.Completion(values = filtered, total = filtered.size, hasMore = false))
}
