package io.github.kamiazya.scopes.platform.observability.logging

interface Logger {
    fun debug(message: String, context: Map<String, Any> = emptyMap())
    fun info(message: String, context: Map<String, Any> = emptyMap())
    fun warn(message: String, context: Map<String, Any> = emptyMap())
    fun error(message: String, context: Map<String, Any> = emptyMap(), throwable: Throwable? = null)

    /**
     * Checks if a given log level is enabled for this logger.
     */
    fun isEnabledFor(level: LogLevel): Boolean

    /**
     * Creates a new Logger instance with additional default context.
     * The returned logger will merge the provided context with any context passed at log time.
     */
    fun withContext(context: Map<String, Any>): Logger

    /**
     * Creates a new Logger instance with a specific name.
     * The name typically represents the class or component using the logger.
     */
    fun withName(name: String): Logger
}
