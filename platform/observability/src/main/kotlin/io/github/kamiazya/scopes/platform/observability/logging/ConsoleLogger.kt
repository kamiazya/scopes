package io.github.kamiazya.scopes.platform.observability.logging

/**
 * Simple console logger implementation.
 * Outputs log messages to the console with structured formatting.
 */
class ConsoleLogger(private val name: String = "ConsoleLogger", private val defaultContext: Map<String, Any> = emptyMap()) : Logger {

    override fun debug(message: String, context: Map<String, Any>) {
        if (isEnabledFor(LogLevel.DEBUG)) {
            val mergedContext = defaultContext + context
            println("[DEBUG] [$name] $message ${formatContext(mergedContext)}")
        }
    }

    override fun info(message: String, context: Map<String, Any>) {
        if (isEnabledFor(LogLevel.INFO)) {
            val mergedContext = defaultContext + context
            println("[INFO] [$name] $message ${formatContext(mergedContext)}")
        }
    }

    override fun warn(message: String, context: Map<String, Any>) {
        if (isEnabledFor(LogLevel.WARN)) {
            val mergedContext = defaultContext + context
            println("[WARN] [$name] $message ${formatContext(mergedContext)}")
        }
    }

    override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {
        if (isEnabledFor(LogLevel.ERROR)) {
            val mergedContext = defaultContext + context
            System.err.println("[ERROR] [$name] $message ${formatContext(mergedContext)}")
            throwable?.printStackTrace()
        }
    }

    override fun isEnabledFor(level: LogLevel): Boolean {
        // For now, log everything. In production, this would be configurable.
        return true
    }

    override fun withContext(context: Map<String, Any>): Logger = ConsoleLogger(name, defaultContext + context)

    override fun withName(name: String): Logger = ConsoleLogger(name, defaultContext)

    private fun formatContext(context: Map<String, Any>): String = if (context.isEmpty()) {
        ""
    } else {
        context.entries.joinToString(prefix = "{ ", postfix = " }") { (key, value) ->
            "$key=$value"
        }
    }
}
