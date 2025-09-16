package io.github.kamiazya.scopes.interfaces.mcp.completions

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
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
    val collector = AliasCompletionCollector(ports, services, prefix, canonicalOnly)
    return collector.collect()
}

private class AliasCompletionCollector(
    private val ports: Ports,
    private val services: Services,
    private val prefix: String,
    private val canonicalOnly: Boolean
) {
    private val q = prefix.lowercase()
    private val visited = mutableSetOf<String>()
    private val aliases = mutableListOf<String>()
    private val titles = mutableListOf<Pair<String, String>>()
    private val byCanonical = mutableMapOf<String, String>()
    private var nodes = 0
    private val maxNodes = 3000
    private val maxDepth = 6
    
    suspend fun collect(): CompleteResult {
        collectScopesFromTree()
        val results = buildCompletionResults()
        
        services.logger.debug("Completions(alias): query='$prefix' nodes=$nodes depth<=$maxDepth -> ${results.size} values")
        return CompleteResult(
            completion = CompleteResult.Completion(values = results, total = results.size, hasMore = false),
        )
    }
    
    private suspend fun collectScopesFromTree() {
        val roots = ports.query.getRootScopes(GetRootScopesQuery(limit = 200))
        val queue = initializeQueue(roots)
        
        while (queue.isNotEmpty() && nodes < maxNodes) {
            val (node, depth) = queue.removeFirst()
            if (depth >= maxDepth) continue
            addChildrenToQueue(node, depth, queue)
        }
    }
    
    private fun initializeQueue(roots: Either<Any, ScopeListResult>): ArrayDeque<Pair<ScopeResult, Int>> {
        val queue = ArrayDeque<Pair<ScopeResult, Int>>()
        roots.fold({ emptyList<ScopeResult>() }, { it.scopes }).forEach { root ->
            if (visited.add(root.id)) {
                addScope(root)
                queue.add(root to 1)
            }
        }
        nodes = queue.size
        return queue
    }
    
    private suspend fun addChildrenToQueue(node: ScopeResult, depth: Int, queue: ArrayDeque<Pair<ScopeResult, Int>>) {
        val children = ports.query.getChildren(GetChildrenQuery(parentId = node.id))
            .fold({ emptyList<ScopeResult>() }, { it.scopes })
            
        for (child in children) {
            if (!visited.add(child.id)) continue
            addScope(child)
            nodes++
            if (nodes >= maxNodes) break
            queue.add(child to (depth + 1))
        }
    }
    
    private fun addScope(scope: ScopeResult) {
        aliases += scope.canonicalAlias
        titles += scope.canonicalAlias to scope.title
        byCanonical[scope.canonicalAlias] = scope.id
    }
    
    private suspend fun buildCompletionResults(): List<String> {
        val distinct = aliases.distinct()
        val matcher = AliasMatcher(distinct, titles, q)
        
        val prefixMatches = matcher.getPrefixMatches()
        val titleMatches = matcher.getTitleMatches(prefixMatches)
        val containsMatches = matcher.getContainsMatches(prefixMatches, titleMatches)
        
        val alternateMatches = if (q.isNotEmpty()) {
            findAlternateAliasMatches(prefixMatches, titleMatches, containsMatches)
        } else emptyList()
        
        return combineAndLimitResults(prefixMatches, titleMatches, containsMatches, alternateMatches)
    }
    
    private suspend fun findAlternateAliasMatches(
        prefixMatches: List<String>,
        titleMatches: List<String>,
        containsMatches: List<String>
    ): List<String> {
        val maxAliasLookups = 300
        var looked = 0
        val altAliasDirect = mutableListOf<String>()
        val altAliasCanonical = mutableListOf<String>()
        
        val remainingCanonicals = titles.asSequence()
            .map { (a, _) -> a }
            .filter { it !in prefixMatches && it !in titleMatches && it !in containsMatches }
            .toList()
            
        for (canonical in remainingCanonicals) {
            if (looked >= maxAliasLookups) break
            val id = byCanonical[canonical] ?: continue
            looked++
            
            val matches = findAliasMatches(id)
            if (matches.isNotEmpty()) {
                if (!canonicalOnly) {
                    altAliasDirect += matches
                } else {
                    altAliasCanonical += canonical
                }
            }
            
            if (altAliasDirect.size + altAliasCanonical.size >= 100) break
        }
        
        return if (!canonicalOnly) altAliasDirect else altAliasCanonical
    }
    
    private suspend fun findAliasMatches(scopeId: String): List<String> {
        val res = ports.query.listAliases(ListAliasesQuery(scopeId = scopeId))
        val alts = res.fold({ emptyList<AliasInfo>() }, { it.aliases })
        return alts.asSequence()
            .map { it.aliasName }
            .filter { it.lowercase().startsWith(q) || it.lowercase().contains(q) }
            .toList()
    }
    
    private fun combineAndLimitResults(
        prefixMatches: List<String>,
        titleMatches: List<String>,
        containsMatches: List<String>,
        alternateMatches: List<String>
    ): List<String> {
        val ordered = buildList {
            addAll(prefixMatches)
            addAll(titleMatches)
            addAll(containsMatches)
        }.distinct()
        
        val combined = (ordered + alternateMatches).distinct()
        return combined.take(100)
    }
}

private class AliasMatcher(
    private val distinct: List<String>,
    private val titles: List<Pair<String, String>>,
    private val q: String
) {
    fun getPrefixMatches(): List<String> =
        distinct.filter { q.isEmpty() || it.lowercase().startsWith(q) }
    
    fun getTitleMatches(excludes: List<String>): List<String> =
        titles.asSequence()
            .filter { (_, t) -> q.isNotEmpty() && t.lowercase().startsWith(q) }
            .map { (a, _) -> a }
            .filter { it !in excludes }
            .distinct()
            .toList()
            
    fun getContainsMatches(vararg excludeLists: List<String>): List<String> {
        val allExcludes = excludeLists.flatMap { it }.toSet()
        return distinct.asSequence()
            .filter { it !in allExcludes }
            .filter { q.isNotEmpty() && it.lowercase().contains(q) }
            .toList()
    }
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
