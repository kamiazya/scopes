package io.github.kamiazya.scopes.infrastructure.logging.formatter

import io.github.kamiazya.scopes.application.logging.LogEntry
import io.github.kamiazya.scopes.application.logging.LogFormatter
import io.github.kamiazya.scopes.application.logging.LogValue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formats log entries as human-readable plain text.
 * Suitable for console output and text log files.
 */
class PlainTextLogFormatter : LogFormatter {

    override fun format(entry: LogEntry): String {
        val timestamp = entry.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val contextString = buildContextString(entry.getAllContext())

        val baseMessage = "[$timestamp] [${entry.level}] [${entry.loggerName}] ${entry.message}$contextString"

        return if (entry.throwable != null) {
            buildString {
                appendLine(baseMessage)
                val throwable = entry.throwable!!
                appendLine("Exception: ${throwable::class.simpleName}: ${throwable.message}")
                throwable.stackTrace.forEach { element ->
                    appendLine("    at $element")
                }
            }.trimEnd()
        } else {
            baseMessage
        }
    }

    private fun buildContextString(context: Map<String, LogValue>): String = if (context.isNotEmpty()) {
        " | " + context.entries.joinToString(", ") { "${it.key}=${formatLogValue(it.value)}" }
    } else {
        ""
    }

    private fun formatLogValue(logValue: LogValue): String = when (logValue) {
        is LogValue.StringValue -> if (logValue.value.contains(' ')) "\"${logValue.value}\"" else logValue.value
        is LogValue.NumberValue -> logValue.value.toString()
        is LogValue.BooleanValue -> logValue.value.toString()
        is LogValue.ListValue -> "[${logValue.value.joinToString(", ") { formatLogValue(it) }}]"
        is LogValue.MapValue -> "{${logValue.value.entries.joinToString(", ") {
            "${it.key}=${formatLogValue(it.value)}"
        }}}"
        LogValue.NullValue -> "null"
    }
}
