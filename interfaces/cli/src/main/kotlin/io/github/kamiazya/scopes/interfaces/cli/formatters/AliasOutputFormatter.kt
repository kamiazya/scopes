package io.github.kamiazya.scopes.interfaces.cli.formatters

import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult

/**
 * Formats alias output for different display formats.
 */
class AliasOutputFormatter {
    private fun formatField(text: String, width: Int): String = if (text.length > width) text.substring(0, width) else text + " ".repeat(width - text.length)

    /**
     * Formats alias list result as a table.
     */
    fun formatAliasListResultAsTable(result: AliasListResult): String = buildString {
        appendLine("┌──────────────────────┬──────────────┬───────────┬────────────────────────┐")
        appendLine("│ Alias                │ Type         │ Canonical │ Created                │")
        appendLine("├──────────────────────┼──────────────┼───────────┼────────────────────────┤")

        result.aliases.sortedBy { it.createdAt }.forEach { alias ->
            val aliasName = formatField(alias.aliasName, 20)
            val aliasType = formatField(alias.aliasType, 12)
            val canonical = formatField(if (alias.isCanonical) "✓" else " ", 9)
            val created = formatField(alias.createdAt.toString(), 22)

            appendLine("│ $aliasName │ $aliasType │ $canonical │ $created │")
        }

        appendLine("└──────────────────────┴──────────────┴───────────┴────────────────────────┘")
        appendLine("Total: ${result.totalCount} aliases")
    }.trim()

    /**
     * Formats alias list result as JSON.
     */
    fun formatAliasListResultAsJson(result: AliasListResult): String = buildString {
        appendLine("{")
        appendLine("  \"scopeId\": \"${result.scopeId}\",")
        appendLine("  \"aliases\": [")
        result.aliases.forEachIndexed { index, alias ->
            appendLine("    {")
            appendLine("      \"aliasName\": \"${alias.aliasName}\",")
            appendLine("      \"aliasType\": \"${alias.aliasType}\",")
            appendLine("      \"isCanonical\": ${alias.isCanonical},")
            appendLine("      \"createdAt\": \"${alias.createdAt}\"")
            append("    }")
            if (index < result.aliases.size - 1) append(",")
            appendLine()
        }
        appendLine("  ],")
        appendLine("  \"totalCount\": ${result.totalCount}")
        appendLine("}")
    }.trim()

    /**
     * Formats alias list result as plain text.
     */
    fun formatAliasListResultAsPlain(result: AliasListResult): String = buildString {
        result.aliases.sortedBy { it.createdAt }.forEach { alias ->
            appendLine(alias.aliasName)
        }
    }.trim()
}
