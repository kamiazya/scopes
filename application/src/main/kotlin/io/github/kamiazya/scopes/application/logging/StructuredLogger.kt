package io.github.kamiazya.scopes.application.logging

import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext

/**
 * Main implementation of the Logger interface that creates structured log entries
 * and delegates formatting and appending to the provided components.
 */
class StructuredLogger(
    private val name: String,
    private val appenders: List<LogAppender>,
    private val defaultContext: Map<String, Any> = emptyMap(),
    private val contextScope: LoggingContextScope = DefaultLoggingContextScope
) : Logger {

    /**
     * Gets the current logging context from coroutine context if available.
     */
    internal suspend fun getCurrentLoggingContext(): LoggingContext? {
        return contextScope.getCurrentContext()
    }

    internal fun log(
        level: LogLevel,
        message: String,
        context: Map<String, Any>,
        throwable: Throwable? = null,
        coroutineContext: LoggingContext? = null
    ) {
        // Check if any appender is enabled for this level
        if (appenders.none { it.isEnabledFor(level) }) {
            return
        }

        val entry = LogEntry(
            timestamp = Clock.System.now(),
            level = level,
            loggerName = name,
            message = message,
            context = (defaultContext + context).toLogValueMap(),
            throwable = throwable,
            coroutineContext = coroutineContext
        )

        appenders.forEach { appender ->
            if (appender.isEnabledFor(level)) {
                appender.append(entry)
            }
        }
    }

    override fun debug(message: String, context: Map<String, Any>) {
        log(LogLevel.DEBUG, message, context)
    }

    override fun info(message: String, context: Map<String, Any>) {
        log(LogLevel.INFO, message, context)
    }

    override fun warn(message: String, context: Map<String, Any>) {
        log(LogLevel.WARN, message, context)
    }

    override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {
        log(LogLevel.ERROR, message, context, throwable)
    }

    override fun isEnabledFor(level: LogLevel): Boolean {
        return appenders.any { it.isEnabledFor(level) }
    }

    override fun withContext(context: Map<String, Any>): Logger {
        return StructuredLogger(name, appenders, defaultContext + context, contextScope)
    }

    override fun withName(name: String): Logger {
        return StructuredLogger(name, appenders, defaultContext, contextScope)
    }
}

/**
 * Coroutine context element for propagating logging context.
 */
class LoggingCoroutineContext(
    val loggingContext: LoggingContext
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<LoggingCoroutineContext>
    override val key: CoroutineContext.Key<*> = Key
}

/**
 * Extension functions for coroutine-aware logging.
 */
suspend fun Logger.debugWithContext(message: String, context: Map<String, Any> = emptyMap()) {
    if (this is StructuredLogger) {
        val loggingContext = this.getCurrentLoggingContext()
        this.log(LogLevel.DEBUG, message, context, coroutineContext = loggingContext)
    } else {
        debug(message, context)
    }
}

suspend fun Logger.infoWithContext(message: String, context: Map<String, Any> = emptyMap()) {
    if (this is StructuredLogger) {
        val loggingContext = this.getCurrentLoggingContext()
        this.log(LogLevel.INFO, message, context, coroutineContext = loggingContext)
    } else {
        info(message, context)
    }
}

suspend fun Logger.warnWithContext(message: String, context: Map<String, Any> = emptyMap()) {
    if (this is StructuredLogger) {
        val loggingContext = this.getCurrentLoggingContext()
        this.log(LogLevel.WARN, message, context, coroutineContext = loggingContext)
    } else {
        warn(message, context)
    }
}

suspend fun Logger.errorWithContext(message: String, context: Map<String, Any> = emptyMap(), throwable: Throwable? = null) {
    if (this is StructuredLogger) {
        val loggingContext = this.getCurrentLoggingContext()
        this.log(LogLevel.ERROR, message, context, throwable, loggingContext)
    } else {
        error(message, context, throwable)
    }
}
