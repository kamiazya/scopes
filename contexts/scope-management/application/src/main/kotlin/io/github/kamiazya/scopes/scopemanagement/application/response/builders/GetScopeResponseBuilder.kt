package io.github.kamiazya.scopes.scopemanagement.application.response.builders

import io.github.kamiazya.scopes.scopemanagement.application.response.data.GetScopeResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GetScopeResponseBuilder : ResponseBuilder<GetScopeResponse> {

    override fun buildMcpResponse(data: GetScopeResponse): Map<String, Any> {
        val json = buildJsonObject {
            put("id", data.scope.id)
            put("canonicalAlias", data.scope.canonicalAlias)
            put("title", data.scope.title)
            data.scope.description?.let { put("description", it) }
            data.scope.parentId?.let { put("parentId", it) }
            put("createdAt", data.scope.createdAt.toString())
            put("updatedAt", data.scope.updatedAt.toString())

            if (!data.scope.aspects.isNullOrEmpty()) {
                putJsonObject("aspects") {
                    data.scope.aspects.forEach { (k, values) ->
                        putJsonArray(k) {
                            values.forEach { value ->
                                add(value)
                            }
                        }
                    }
                }
            }

            if (data.aliases?.isNotEmpty() == true) {
                putJsonArray("aliases") {
                    data.aliases.forEach { alias ->
                        add(
                            buildJsonObject {
                                put("name", alias.aliasName)
                                put("isCanonical", alias.isCanonical)
                            },
                        )
                    }
                }
            }
        }

        return Json.parseToJsonElement(json.toString()).jsonObject.toMap()
    }

    override fun buildCliResponse(data: GetScopeResponse): String = buildString {
        appendTitleSection(data)
        appendDescriptionSection(data.scope)
        appendAliasesSection(data)
        appendAspectsSection(data.scope)
        appendTimestampsSection(data)
        appendParentSection(data)
    }.trim()

    private fun StringBuilder.appendTitleSection(data: GetScopeResponse) {
        val scope = data.scope
        appendLine("${scope.title}")
        if (data.includeDebug) {
            appendLine("ID: ${scope.id}")
        }
        appendLine()
    }

    private fun StringBuilder.appendDescriptionSection(scope: io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult) {
        scope.description?.let {
            appendLine("Description: $it")
            appendLine()
        }
    }

    private fun StringBuilder.appendAliasesSection(data: GetScopeResponse) {
        if (data.aliases?.isNotEmpty() == true) {
            appendLine("Aliases:")
            data.aliases.forEach { alias ->
                val typeLabel = if (alias.isCanonical) " (canonical)" else ""
                if (data.includeDebug && alias.isCanonical) {
                    appendLine("  ${alias.aliasName}$typeLabel (ULID: ${data.scope.id})")
                } else {
                    appendLine("  ${alias.aliasName}$typeLabel")
                }
            }
        } else {
            appendLine("Aliases: ${data.scope.canonicalAlias}")
        }
        appendLine()
    }

    private fun StringBuilder.appendAspectsSection(scope: io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult) {
        if (!scope.aspects.isNullOrEmpty()) {
            appendLine("Aspects:")
            scope.aspects.forEach { (key, value) ->
                appendLine("  $key: ${value.joinToString(", ")}")
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendTimestampsSection(data: GetScopeResponse) {
        if (data.includeTimestamps) {
            appendLine("Created: ${data.scope.createdAt}")
            appendLine("Updated: ${data.scope.updatedAt}")
            appendLine()
        }
    }

    private fun StringBuilder.appendParentSection(data: GetScopeResponse) {
        if (data.includeDebug && data.scope.parentId != null) {
            appendLine("Parent: ${data.scope.parentId}")
        }
    }
}
