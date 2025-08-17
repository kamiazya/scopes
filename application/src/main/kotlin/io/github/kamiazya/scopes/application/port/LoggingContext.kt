package io.github.kamiazya.scopes.application.port

import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Pure data class representing logging context.
 * This is independent of any coroutine or threading mechanism.
 */
data class LoggingContext(
    val sessionId: String = generateSessionId(),
    val loggerName: String? = null,
    val operationType: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Creates a new LoggingContext with additional metadata.
     */
    fun withMetadata(additionalMetadata: Map<String, Any>): LoggingContext {
        return copy(metadata = metadata + additionalMetadata)
    }

    /**
     * Creates a new LoggingContext with a specific logger name.
     */
    fun withLoggerName(name: String): LoggingContext {
        return copy(loggerName = name)
    }

    /**
     * Converts this context to a map suitable for logging.
     */
    fun toMap(): Map<String, Any> {
        return buildMap {
            put("sessionId", sessionId)
            loggerName?.let { put("logger", it) }
            operationType?.let { put("operationType", it) }
            putAll(metadata)
        }
    }
    
    companion object {
        /**
         * Generates a unique session ID.
         */
        private fun generateSessionId(): String {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val random = Random.nextInt(1000, 9999)
            return "$timestamp-$random"
        }
    }
}