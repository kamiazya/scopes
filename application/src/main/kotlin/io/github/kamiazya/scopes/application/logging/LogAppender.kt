package io.github.kamiazya.scopes.application.logging

/**
 * Interface for appending log entries to various destinations.
 * Implementations can write to console, files, remote services, etc.
 */
interface LogAppender {
    /**
     * Appends a log entry to the destination.
     *
     * @param entry The log entry to append
     */
    fun append(entry: LogEntry)

    /**
     * Checks if this appender is enabled for the given log level.
     *
     * @param level The log level to check
     * @return true if the appender should process entries at this level
     */
    fun isEnabledFor(level: LogLevel): Boolean

    /**
     * Sets the minimum log level for this appender.
     *
     * @param level The minimum log level to process
     */
    fun setLevel(level: LogLevel)
}
