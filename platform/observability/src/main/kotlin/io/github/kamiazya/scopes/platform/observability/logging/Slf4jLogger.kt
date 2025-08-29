package io.github.kamiazya.scopes.platform.observability.logging

import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * SLF4J-based logger implementation.
 * Delegates logging to SLF4J while maintaining the structured context approach.
 */
class Slf4jLogger(private val name: String = "Slf4jLogger", private val defaultContext: Map<String, Any> = emptyMap()) : Logger {

    private val slf4jLogger = LoggerFactory.getLogger(name)

    override fun debug(message: String, context: Map<String, Any>) {
        if (isEnabledFor(LogLevel.DEBUG)) {
            withMDC(defaultContext + context) {
                slf4jLogger.debug(message)
            }
        }
    }

    override fun info(message: String, context: Map<String, Any>) {
        if (isEnabledFor(LogLevel.INFO)) {
            withMDC(defaultContext + context) {
                slf4jLogger.info(message)
            }
        }
    }

    override fun warn(message: String, context: Map<String, Any>) {
        if (isEnabledFor(LogLevel.WARN)) {
            withMDC(defaultContext + context) {
                slf4jLogger.warn(message)
            }
        }
    }

    override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {
        if (isEnabledFor(LogLevel.ERROR)) {
            withMDC(defaultContext + context) {
                if (throwable != null) {
                    slf4jLogger.error(message, throwable)
                } else {
                    slf4jLogger.error(message)
                }
            }
        }
    }

    override fun isEnabledFor(level: LogLevel): Boolean = when (level) {
        LogLevel.DEBUG -> slf4jLogger.isDebugEnabled
        LogLevel.INFO -> slf4jLogger.isInfoEnabled
        LogLevel.WARN -> slf4jLogger.isWarnEnabled
        LogLevel.ERROR -> slf4jLogger.isErrorEnabled
    }

    override fun withContext(context: Map<String, Any>): Logger = Slf4jLogger(name, defaultContext + context)

    override fun withName(name: String): Logger = Slf4jLogger(name, defaultContext)

    /**
     * Executes the given block with the provided context set in MDC.
     * The context is automatically cleared after the block execution.
     */
    private inline fun <T> withMDC(context: Map<String, Any>, block: () -> T): T {
        val previousValues = mutableMapOf<String, String?>()

        try {
            // Set context values in MDC
            context.forEach { (key, value) ->
                previousValues[key] = MDC.get(key)
                MDC.put(key, value.toString())
            }

            return block()
        } finally {
            // Restore previous MDC values
            context.keys.forEach { key ->
                val previousValue = previousValues[key]
                if (previousValue != null) {
                    MDC.put(key, previousValue)
                } else {
                    MDC.remove(key)
                }
            }
        }
    }
}
