package io.github.kamiazya.scopes.infrastructure.logger

import io.github.kamiazya.scopes.application.port.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Simple logger implementation that outputs to console.
 * For coroutine context support, use the extension functions in SuspendLogger.
 */
class ConsoleLogger(
    private val defaultLoggerName: String = "Application"
) : Logger {

    // Implementation of Logger interface
    override fun debug(message: String, context: Map<String, Any>) {
        log("DEBUG", defaultLoggerName, message, context)
    }

    override fun info(message: String, context: Map<String, Any>) {
        log("INFO", defaultLoggerName, message, context)
    }

    override fun warn(message: String, context: Map<String, Any>) {
        log("WARN", defaultLoggerName, message, context)
    }

    override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {
        log("ERROR", defaultLoggerName, message, context, throwable)
    }

    override fun withContext(context: Map<String, Any>): Logger {
        // This creates a wrapper that adds context
        return object : Logger {
            override fun debug(message: String, additionalContext: Map<String, Any>) {
                this@ConsoleLogger.debug(message, context + additionalContext)
            }

            override fun info(message: String, additionalContext: Map<String, Any>) {
                this@ConsoleLogger.info(message, context + additionalContext)
            }

            override fun warn(message: String, additionalContext: Map<String, Any>) {
                this@ConsoleLogger.warn(message, context + additionalContext)
            }

            override fun error(message: String, additionalContext: Map<String, Any>, throwable: Throwable?) {
                this@ConsoleLogger.error(message, context + additionalContext, throwable)
            }

            override fun withContext(additionalContext: Map<String, Any>): Logger {
                return this@ConsoleLogger.withContext(context + additionalContext)
            }

            override fun withName(name: String): Logger {
                return this@ConsoleLogger.withName(name)
            }
        }
    }

    override fun withName(name: String): Logger {
        return ConsoleLogger(name)
    }

    private fun log(
        level: String,
        loggerName: String,
        message: String,
        context: Map<String, Any>,
        throwable: Throwable? = null
    ) {
        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        val contextString = if (context.isNotEmpty()) {
            " | ${context.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        } else {
            ""
        }

        println("[$timestamp] [$level] [$loggerName] $message$contextString")

        throwable?.let {
            println("Exception: ${it.message}")
            it.printStackTrace()
        }
    }
}
