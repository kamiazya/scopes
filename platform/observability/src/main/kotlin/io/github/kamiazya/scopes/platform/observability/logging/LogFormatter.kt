package io.github.kamiazya.scopes.platform.observability.logging

/**
 * Interface for formatting log entries into different output formats.
 * Implementations can produce JSON, plain text, OpenTelemetry format, etc.
 */
interface LogFormatter {
    /**
     * Formats a log entry into a string representation.
     *
     * @param entry The log entry to format
     * @return The formatted string representation of the log entry
     */
    fun format(entry: LogEntry): String
}
