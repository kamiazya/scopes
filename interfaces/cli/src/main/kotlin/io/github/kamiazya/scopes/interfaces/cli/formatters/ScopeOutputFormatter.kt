package io.github.kamiazya.scopes.interfaces.cli.formatters

import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
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
     * Formats alias for user-friendly display by removing @ prefix
     */
    private fun formatAlias(alias: String): String = alias.removePrefix("@")

    /**
     * Formats a deletion result
     */
    fun formatDeleteResult(id: String): String = "Scope '$id' deleted successfully."

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

    /**
     * Formats a contract create result with debug information when enabled
     */
    fun formatContractCreateResult(result: ContractCreateScopeResult, debugMode: Boolean = false): String = buildString {
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
            val displayAlias = formatAlias(it)
            if (debugMode) {
                appendLine("Alias: $displayAlias (ULID: ${result.id})")
            } else {
                appendLine("Alias: $displayAlias")
            }
        }
    }.trim()

    /**
     * Formats a contract scope result with debug information when enabled
     */
    fun formatContractScope(scope: ScopeResult, debugMode: Boolean = false): String = buildString {
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
        val displayAlias = formatAlias(scope.canonicalAlias)
        if (debugMode) {
            appendLine("Canonical Alias: ★ $displayAlias (ULID: ${scope.id})")
        } else {
            appendLine("Canonical Alias: ★ $displayAlias")
        }
        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a contract scope result with all aliases and debug information when enabled
     */
    fun formatContractScopeWithAliases(scope: ScopeResult, aliasResult: AliasListResult, debugMode: Boolean = false): String = buildString {
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

        // Show aliases with canonical emphasized
        if (aliasResult.aliases.isNotEmpty()) {
            val canonicalAliases = aliasResult.aliases.filter { it.isCanonical }
            val customAliases = aliasResult.aliases.filter { !it.isCanonical }

            // Show canonical alias prominently
            canonicalAliases.forEach { alias ->
                val displayAlias = formatAlias(alias.aliasName)
                if (debugMode) {
                    appendLine("Canonical Alias: ★ $displayAlias (ULID: ${scope.id})")
                } else {
                    appendLine("Canonical Alias: ★ $displayAlias")
                }
            }

            // Show custom aliases if any
            if (customAliases.isNotEmpty()) {
                appendLine("Custom Aliases (${customAliases.size}):")
                customAliases.forEach { alias ->
                    val displayAlias = formatAlias(alias.aliasName)
                    if (debugMode) {
                        appendLine("  • $displayAlias (ULID: ${scope.id})")
                    } else {
                        appendLine("  • $displayAlias")
                    }
                }
            }
        } else {
            appendLine("Aliases: None")
        }

        appendLine("Created: ${scope.createdAt}")
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a contract update result with debug information when enabled
     */
    fun formatContractUpdateResult(scope: ScopeResult, debugMode: Boolean = false): String = buildString {
        appendLine("Scope updated successfully!")
        if (debugMode) {
            appendLine("ID: ${scope.id}")
        }
        appendLine("Title: ${scope.title}")
        scope.description?.let { appendLine("Description: $it") }
        appendLine("Updated: ${scope.updatedAt}")
    }.trim()

    /**
     * Formats a list of contract scope results with debug information when enabled
     */
    fun formatContractScopeList(scopes: List<ScopeResult>, debugMode: Boolean = false): String {
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
    private fun formatContractScopeListItem(scope: ScopeResult, debugMode: Boolean = false): String = buildString {
        append("• ${scope.title}")
        scope.description?.let {
            append(" - $it")
        }
        appendLine()
        val displayAlias = formatAlias(scope.canonicalAlias)
        if (debugMode) {
            append("  ID: ${scope.id}")
            append(" | Alias: ★ $displayAlias (ULID: ${scope.id})")
        } else {
            append("  Alias: ★ $displayAlias")
        }
        scope.parentId?.let {
            if (debugMode) {
                append(" | Parent: $it")
            }
        }
    }
}
