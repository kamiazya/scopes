package io.github.kamiazya.scopes.scopemanagement.application.response.builders

import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.response.data.ListScopesResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class ListScopesResponseBuilder : ResponseBuilder<ListScopesResponse> {

    override fun buildMcpResponse(data: ListScopesResponse): Map<String, Any> {
        val json = buildJsonObject {
            // Use appropriate field name based on response type
            val arrayFieldName = if (data.isRootScopes) "roots" else "scopes"

            putJsonArray(arrayFieldName) {
                data.scopes.forEach { scope ->
                    add(buildScopeJson(scope))
                }
            }
            data.totalCount?.let { put("totalCount", it) }
            data.hasMore?.let { put("hasMore", it) }
        }

        return Json.parseToJsonElement(json.toString()).jsonObject.toMap()
    }

    override fun buildCliResponse(data: ListScopesResponse): String {
        if (data.scopes.isEmpty()) {
            return "No scopes found."
        }

        return buildString {
            if (data.totalCount != null) {
                appendLine("Found ${data.totalCount} scope(s) (showing ${data.scopes.size}):")
            } else {
                appendLine("Found ${data.scopes.size} scope(s):")
            }
            appendLine()

            data.scopes.forEachIndexed { index, scope ->
                if (index > 0) appendLine()
                appendLine(formatScope(scope, data.includeAliases, data.includeDebug))
            }

            if (data.hasMore == true) {
                appendLine()
                appendLine("(More scopes available - use --offset/--limit to paginate)")
            }
        }.trim()
    }

    private fun buildScopeJson(scope: ScopeResult): JsonObject = buildJsonObject {
        put("id", scope.id)
        put("canonicalAlias", scope.canonicalAlias)
        put("title", scope.title)
        scope.description?.let { put("description", it) }
        scope.parentId?.let { put("parentId", it) }

        if (!scope.aspects.isNullOrEmpty()) {
            putJsonObject("aspects") {
                scope.aspects.forEach { (k, values) ->
                    putJsonArray(k) {
                        values.forEach { value ->
                            add(value)
                        }
                    }
                }
            }
        }
    }

    private fun formatScope(scope: ScopeResult, includeAliases: Boolean, includeDebug: Boolean): String = buildString {
        append("• ${scope.title}")
        if (includeDebug) {
            append(" (ID: ${scope.id})")
        }

        scope.description?.let {
            appendLine()
            append("  Description: $it")
        }

        if (includeAliases) {
            appendLine()
            append("  Aliases: ${scope.canonicalAlias}")
            if (includeDebug) {
                append(" (ULID: ${scope.id})")
            }
        }

        if (!scope.aspects.isNullOrEmpty()) {
            appendLine()
            append("  Aspects: ")
            append(scope.aspects.entries.joinToString(", ") { "${it.key}:${it.value}" })
        }

        if (includeDebug && scope.parentId != null) {
            appendLine()
            append("  Parent: ${scope.parentId}")
        }
    }
}
