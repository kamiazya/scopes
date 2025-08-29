package io.github.kamiazya.scopes.interfaces.cli.formatters

import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult as ContractCreateScopeResult

/**
 * Formatter for CLI output of scope-related data
 *
 * This formatter is responsible for converting domain DTOs into
 * human-readable CLI output. It provides consistent formatting
 * across all scope-related commands.
 *
 * Key responsibilities:
 * - Format scope data for CLI display
 * - Handle different output modes (verbose, quiet, json)
 * - Provide consistent styling and structure
 */
class ScopeOutputFormatter {

    /**
     * Formats a created scope result
     */
    fun formatCreateResult(result: CreateScopeResult): String = buildString {
        appendLine("Scope created successfully!")
        appendLine("ID: ${result.id}")
        appendLine("Title: ${result.title}")
        result.description?.let { appendLine("Description: $it") }
        result.parentId?.let { appendLine("Parent ID: $it") }
        result.canonicalAlias?.let { appendLine("Alias: $it") }
    }.trim()

    /**
     * Formats a single scope
     */
    fun formatScope(scope: ScopeDto): String = buildString {
        appendLine("Scope Details:")
        appendLine("ID: ${scope.id}")
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        scope.parentId?.let { appendLine("Parent ID: $it") }
        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
        if (scope.aspects.isNotEmpty()) {
            appendLine("Aspects:")
            scope.aspects.forEach { (key, values) ->
                appendLine("  $key: ${values.joinToString(", ")}")
            }
        }
    }.trim()

    /**
     * Formats a list of scopes
     */
    fun formatScopeList(scopes: List<ScopeDto>): String {
        if (scopes.isEmpty()) {
            return "No scopes found."
        }

        return buildString {
            appendLine("Found ${scopes.size} scope(s):")
            appendLine()

            scopes.forEachIndexed { index, scope ->
                if (index > 0) appendLine()
                appendLine(formatScopeListItem(scope))
            }
        }.trim()
    }

    /**
     * Formats a single scope for list display
     */
    private fun formatScopeListItem(scope: ScopeDto): String = buildString {
        append("• ${scope.title}")
        scope.description?.let {
            append(" - $it")
        }
        appendLine()
        append("  ID: ${scope.id}")
        scope.parentId?.let {
            append(" | Parent: $it")
        }
    }

    /**
     * Formats an update result
     */
    fun formatUpdateResult(scope: ScopeDto): String = buildString {
        appendLine("Scope updated successfully!")
        appendLine("ID: ${scope.id}")
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a deletion result
     */
    fun formatDeleteResult(id: String): String = "Scope '$id' deleted successfully."

    // Debug-aware formatters

    /**
     * Formats a created scope result with debug information when enabled
     */
    fun formatCreateResult(result: CreateScopeResult, debugMode: Boolean): String = buildString {
        appendLine("Scope created successfully!")
        if (debugMode) {
            appendLine("ID: ${result.id}")
        }
        appendLine("Title: ${result.title}")
        result.description?.let { appendLine("Description: $it") }
        result.parentId?.let {
            if (debugMode) {
                appendLine("Parent ID: $it")
            }
        }
        result.canonicalAlias?.let {
            if (debugMode) {
                appendLine("Alias: $it (ULID: ${result.id})")
            } else {
                appendLine("Alias: $it")
            }
        }
    }.trim()

    /**
     * Formats a single scope with debug information when enabled
     */
    fun formatScope(scope: ScopeDto, debugMode: Boolean): String = buildString {
        appendLine("Scope Details:")
        if (debugMode) {
            appendLine("ID: ${scope.id}")
        }
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        scope.parentId?.let {
            if (debugMode) {
                appendLine("Parent ID: $it")
            }
        }
        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
        if (scope.aspects.isNotEmpty()) {
            appendLine("Aspects:")
            scope.aspects.forEach { (key, values) ->
                appendLine("  $key: ${values.joinToString(", ")}")
            }
        }
    }.trim()

    /**
     * Formats a list of scopes with debug information when enabled
     */
    fun formatScopeList(scopes: List<ScopeDto>, debugMode: Boolean): String {
        if (scopes.isEmpty()) {
            return "No scopes found."
        }

        return buildString {
            appendLine("Found ${scopes.size} scope(s):")
            appendLine()

            scopes.forEachIndexed { index, scope ->
                if (index > 0) appendLine()
                appendLine(formatScopeListItem(scope, debugMode))
            }
        }.trim()
    }

    /**
     * Formats a single scope for list display with debug information when enabled
     */
    private fun formatScopeListItem(scope: ScopeDto, debugMode: Boolean): String = buildString {
        append("• ${scope.title}")
        scope.description?.let {
            append(" - $it")
        }
        appendLine()
        if (debugMode) {
            append("  ID: ${scope.id}")
        }
        scope.parentId?.let {
            if (debugMode) {
                append(" | Parent: $it")
            }
        }
    }

    /**
     * Formats an update result with debug information when enabled
     */
    fun formatUpdateResult(scope: ScopeDto, debugMode: Boolean): String = buildString {
        appendLine("Scope updated successfully!")
        if (debugMode) {
            appendLine("ID: ${scope.id}")
        }
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    // Contract result formatters

    /**
     * Formats a contract create result
     */
    fun formatContractCreateResult(result: ContractCreateScopeResult): String = buildString {
        appendLine("Scope created successfully!")
        appendLine("ID: ${result.id}")
        appendLine("Title: ${result.title}")
        result.description?.let { appendLine("Description: $it") }
        result.parentId?.let { appendLine("Parent ID: $it") }
        result.canonicalAlias?.let { appendLine("Alias: $it") }
    }.trim()

    /**
     * Formats a contract scope result
     */
    fun formatContractScope(scope: ScopeResult): String = buildString {
        appendLine("Scope Details:")
        appendLine("ID: ${scope.id}")
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        scope.parentId?.let { appendLine("Parent ID: $it") }
        appendLine("Canonical Alias: ${scope.canonicalAlias}")
        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a contract scope result with all aliases
     */
    fun formatContractScopeWithAliases(scope: ScopeResult, aliasResult: AliasListResult): String = buildString {
        appendLine("Scope Details:")
        appendLine("ID: ${scope.id}")
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        scope.parentId?.let { appendLine("Parent ID: $it") }

        // Show all aliases
        if (aliasResult.aliases.isNotEmpty()) {
            appendLine("Aliases (${aliasResult.totalCount}):")
            aliasResult.aliases.forEach { alias ->
                val typeLabel = when {
                    alias.isCanonical -> " (canonical)"
                    else -> " (custom)"
                }
                appendLine("  - ${alias.aliasName}$typeLabel")
            }
        } else {
            appendLine("Aliases: None")
        }

        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a contract update result
     */
    fun formatContractUpdateResult(scope: ScopeResult): String = buildString {
        appendLine("Scope updated successfully!")
        appendLine("ID: ${scope.id}")
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a list of contract scope results
     */
    fun formatContractScopeList(scopes: List<ScopeResult>): String {
        if (scopes.isEmpty()) {
            return "No scopes found."
        }

        return buildString {
            appendLine("Found ${scopes.size} scope(s):")
            appendLine()

            scopes.forEachIndexed { index, scope ->
                if (index > 0) appendLine()
                appendLine(formatContractScopeListItem(scope))
            }
        }.trim()
    }

    /**
     * Formats a single contract scope for list display
     */
    private fun formatContractScopeListItem(scope: ScopeResult): String = buildString {
        append("• ${scope.title}")
        scope.description?.let {
            append(" - $it")
        }
        appendLine()
        append("  ID: ${scope.id}")
        append(" | Alias: ${scope.canonicalAlias}")
        scope.parentId?.let {
            append(" | Parent: $it")
        }
    }

    /**
     * Formats scope tree structure (for future hierarchical display)
     */
    fun formatScopeTree(scopes: List<ScopeDto>, rootId: String? = null, indent: String = ""): String = buildString {
        val children = if (rootId == null) {
            scopes.filter { it.parentId == null }
        } else {
            scopes.filter { it.parentId == rootId }
        }

        children.forEach { scope ->
            appendLine("$indent${if (indent.isEmpty()) "•" else "└─"} ${scope.title}")
            val childTree = formatScopeTree(
                scopes,
                scope.id,
                "$indent  ",
            )
            if (childTree.isNotEmpty()) {
                append(childTree)
            }
        }
    }.trim()

    // Debug-aware contract result formatters

    /**
     * Formats a contract create result with debug information when enabled
     */
    fun formatContractCreateResult(result: ContractCreateScopeResult, debugMode: Boolean): String = buildString {
        appendLine("Scope created successfully!")
        if (debugMode) {
            appendLine("ID: ${result.id}")
        }
        appendLine("Title: ${result.title}")
        result.description?.let { appendLine("Description: $it") }
        result.parentId?.let {
            if (debugMode) {
                appendLine("Parent ID: $it")
            }
        }
        result.canonicalAlias?.let {
            if (debugMode) {
                appendLine("Alias: $it (ULID: ${result.id})")
            } else {
                appendLine("Alias: $it")
            }
        }
    }.trim()

    /**
     * Formats a contract scope result with debug information when enabled
     */
    fun formatContractScope(scope: ScopeResult, debugMode: Boolean): String = buildString {
        appendLine("Scope Details:")
        if (debugMode) {
            appendLine("ID: ${scope.id}")
        }
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        scope.parentId?.let {
            if (debugMode) {
                appendLine("Parent ID: $it")
            }
        }
        if (debugMode) {
            appendLine("Canonical Alias: ${scope.canonicalAlias} (ULID: ${scope.id})")
        } else {
            appendLine("Canonical Alias: ${scope.canonicalAlias}")
        }
        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a contract scope result with all aliases and debug information when enabled
     */
    fun formatContractScopeWithAliases(scope: ScopeResult, aliasResult: AliasListResult, debugMode: Boolean): String = buildString {
        appendLine("Scope Details:")
        if (debugMode) {
            appendLine("ID: ${scope.id}")
        }
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        scope.parentId?.let {
            if (debugMode) {
                appendLine("Parent ID: $it")
            }
        }

        // Show all aliases
        if (aliasResult.aliases.isNotEmpty()) {
            appendLine("Aliases (${aliasResult.totalCount}):")
            aliasResult.aliases.forEach { alias ->
                val typeLabel = when {
                    alias.isCanonical -> " (canonical)"
                    else -> " (custom)"
                }
                if (debugMode) {
                    appendLine("  - ${alias.aliasName}$typeLabel (ULID: ${scope.id})")
                } else {
                    appendLine("  - ${alias.aliasName}$typeLabel")
                }
            }
        } else {
            appendLine("Aliases: None")
        }

        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a list of contract scope results with debug information when enabled
     */
    fun formatContractScopeList(scopes: List<ScopeResult>, debugMode: Boolean): String {
        if (scopes.isEmpty()) {
            return "No scopes found."
        }

        return buildString {
            appendLine("Found ${scopes.size} scope(s):")
            appendLine()

            scopes.forEachIndexed { index, scope ->
                if (index > 0) appendLine()
                appendLine(formatContractScopeListItem(scope, debugMode))
            }
        }.trim()
    }

    /**
     * Formats a single contract scope for list display with debug information when enabled
     */
    private fun formatContractScopeListItem(scope: ScopeResult, debugMode: Boolean): String = buildString {
        append("• ${scope.title}")
        scope.description?.let {
            append(" - $it")
        }
        appendLine()
        if (debugMode) {
            append("  ID: ${scope.id}")
            append(" | Alias: ${scope.canonicalAlias} (ULID: ${scope.id})")
        } else {
            append("  Alias: ${scope.canonicalAlias}")
        }
        scope.parentId?.let {
            if (debugMode) {
                append(" | Parent: $it")
            }
        }
    }
}
