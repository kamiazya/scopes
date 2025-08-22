package io.github.kamiazya.scopes.platform.observability.logging.appender

import io.github.kamiazya.scopes.platform.observability.logging.LogAppender
import io.github.kamiazya.scopes.platform.observability.logging.LogEntry
import io.github.kamiazya.scopes.platform.observability.logging.LogFormatter
import io.github.kamiazya.scopes.platform.observability.logging.LogLevel

/**
 * Appends formatted log entries to the console (stdout/stderr).
 * Error level logs are written to stderr, others to stdout.
 */
class ConsoleLogAppender(private val formatter: LogFormatter, private var minLevel: LogLevel = LogLevel.DEBUG) : LogAppender {

    override fun append(entry: LogEntry) {
        val formattedMessage = formatter.format(entry)

        when (entry.level) {
            LogLevel.ERROR -> System.err.println(formattedMessage)
            else -> println(formattedMessage)
        }
    }

    override fun isEnabledFor(level: LogLevel): Boolean = level.ordinal >= minLevel.ordinal

    override fun setLevel(level: LogLevel) {
        minLevel = level
    }
}
