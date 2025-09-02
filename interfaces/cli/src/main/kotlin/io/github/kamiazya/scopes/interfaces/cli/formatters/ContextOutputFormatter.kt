package io.github.kamiazya.scopes.interfaces.cli.formatters

import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextView
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formatter for context view output in CLI.
 */
class ContextOutputFormatter {

    /**
     * Format a single context view for display.
     */
    fun formatContextView(context: ContextView, debug: Boolean = false): String = buildString {
        appendLine("Key: ${context.key}")
        appendLine("Name: ${context.name}")
        context.description?.let { appendLine("Description: $it") }
        appendLine("Filter: ${context.filter}")

        if (debug) {
            appendLine("Created: ${formatTimestamp(context.createdAt)}")
            appendLine("Updated: ${formatTimestamp(context.updatedAt)}")
        }
    }

    /**
     * Format a detailed view of a context.
     */
    fun formatContextViewDetailed(context: ContextView, debug: Boolean = false): String = buildString {
        appendLine("Context View Details")
        appendLine("===================")
        appendLine()
        appendLine("Key: ${context.key}")
        appendLine("Name: ${context.name}")
        context.description?.let {
            appendLine("Description: $it")
        }
        appendLine()
        appendLine("Filter Expression:")
        appendLine("  ${context.filter}")
        appendLine()
        appendLine("Timestamps:")
        appendLine("  Created: ${formatTimestamp(context.createdAt)}")
        appendLine("  Updated: ${formatTimestamp(context.updatedAt)}")

        if (debug) {
            appendLine()
            appendLine("Debug Information:")
            appendLine("  Filter (raw): ${context.filter}")
        }
    }

    /**
     * Format a list of context views.
     */
    fun formatContextList(contexts: List<ContextView>, currentKey: String? = null, debug: Boolean = false): String = buildString {
        appendLine("Context Views (${contexts.size}):")
        appendLine()

        contexts.forEach { context ->
            val marker = if (context.key == currentKey) " [CURRENT]" else ""
            appendLine("• ${context.key}$marker - ${context.name}")
            context.description?.let { desc ->
                appendLine("  $desc")
            }
            appendLine("  Filter: ${context.filter}")

            if (debug) {
                appendLine("  Created: ${formatTimestamp(context.createdAt)}")
            }
            appendLine()
        }

        if (contexts.isEmpty()) {
            appendLine("No context views defined.")
        }
    }.trimEnd()

    private fun formatTimestamp(instant: Instant): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.date} ${localDateTime.time.toString().substringBeforeLast(".")}"
    }
}
