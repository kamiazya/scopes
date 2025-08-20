package io.github.kamiazya.scopes.application.logging

import kotlinx.datetime.Instant

/**
 * Represents a structured log entry containing all necessary information for logging.
 * This is the internal representation used by the logging system.
 */
data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val loggerName: String,
    val message: String,
    val context: Map<String, LogValue> = emptyMap(),
    val throwable: Throwable? = null,
    val coroutineContext: LoggingContext? = null,
    val applicationInfo: ApplicationInfo? = null,
    val runtimeInfo: RuntimeInfo? = null,
) {
    /**
     * Merges this log entry with additional context.
     */
    fun withAdditionalContext(additionalContext: Map<String, LogValue>): LogEntry = copy(context = context + additionalContext)

    /**
     * Returns all context including coroutine context, application info, and runtime info as a single map.
     */
    fun getAllContext(): Map<String, LogValue> = buildMap {
        putAll(context)

        // Add coroutine context if available
        coroutineContext?.let {
            putAll(it.toLogValueMap())
        }

        // Add application info if available
        applicationInfo?.let {
            putAll(it.toLogValueMap())
        }

        // Add runtime info if available
        runtimeInfo?.let {
            putAll(it.toLogValueMap())
        }
    }
}
