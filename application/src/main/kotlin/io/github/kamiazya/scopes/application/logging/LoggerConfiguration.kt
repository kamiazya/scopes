package io.github.kamiazya.scopes.application.logging

/**
 * DSL marker for logger configuration.
 */
@DslMarker
annotation class LoggerDsl

/**
 * Configuration class for building loggers using Kotlin DSL.
 */
@LoggerDsl
class LoggerConfiguration {
    var name: String = "Application"
    internal var applicationInfo: ApplicationInfo? = null
    internal var runtimeInfo: RuntimeInfo? = null
    internal val appenders = mutableListOf<LogAppender>()
    internal val defaultContext = mutableMapOf<String, Any>()
    internal var contextScope: LoggingContextScope? = null

    /**
     * Configures application information.
     */
    fun application(block: ApplicationInfoBuilder.() -> Unit) {
        applicationInfo = ApplicationInfoBuilder().apply(block).build()
    }

    /**
     * Sets runtime information.
     */
    fun runtime(info: RuntimeInfo) {
        runtimeInfo = info
    }

    /**
     * Adds an appender to the logger configuration.
     */
    fun appender(appender: LogAppender) {
        appenders.add(appender)
    }

    /**
     * Configures a console appender.
     */
    fun console(block: ConsoleAppenderConfiguration.() -> Unit = {}) {
        val config = ConsoleAppenderConfiguration().apply(block)
        appenders.add(config.build())
    }

    /**
     * Adds context values.
     */
    fun context(block: MutableMap<String, Any>.() -> Unit) {
        defaultContext.apply(block)
    }

    /**
     * Adds a single context value.
     */
    fun context(key: String, value: Any) {
        defaultContext[key] = value
    }

    /**
     * Sets the logging context scope for coroutine context propagation.
     */
    fun contextScope(scope: LoggingContextScope) {
        contextScope = scope
    }
}

/**
 * Builder for ApplicationInfo using DSL.
 */
@LoggerDsl
class ApplicationInfoBuilder {
    var name: String = ""
    var version: String = "dev"
    var type: ApplicationType = ApplicationType.CUSTOM
    private val metadata = mutableMapOf<String, Any>()

    /**
     * Adds custom metadata.
     */
    fun metadata(block: MutableMap<String, Any>.() -> Unit) {
        metadata.apply(block)
    }

    /**
     * Adds a single metadata value.
     */
    fun metadata(key: String, value: Any) {
        metadata[key] = value
    }

    internal fun build(): ApplicationInfo {
        require(name.isNotEmpty()) { "Application name must be specified" }
        return ApplicationInfo(
            name = name,
            version = version,
            type = type,
            customMetadata = metadata.toMap()
        )
    }
}

/**
 * Configuration for console appender.
 */
@LoggerDsl
class ConsoleAppenderConfiguration {
    var formatter: LogFormatter? = null
    var level: LogLevel = LogLevel.DEBUG

    /**
     * Uses JSON formatter.
     */
    fun json() {
        formatter = JsonLogFormatter()
    }

    /**
     * Uses plain text formatter.
     */
    fun plainText() {
        formatter = PlainTextLogFormatter()
    }

    internal fun build(): LogAppender {
        val finalFormatter = formatter ?: PlainTextLogFormatter()
        return ConsoleLogAppender(finalFormatter).apply {
            setLevel(level)
        }
    }
}

/**
 * Creates a logger using DSL configuration.
 */
fun logger(block: LoggerConfiguration.() -> Unit): Logger {
    val config = LoggerConfiguration().apply(block)

    require(config.appenders.isNotEmpty()) {
        "At least one appender must be configured. Use console { } or appender(...)"
    }

    // Build the default context including application and runtime info
    val enrichedContext = buildMap {
        putAll(config.defaultContext)

        // Add application info to default context if provided
        config.applicationInfo?.let { info ->
            putAll(info.toMap())
        }

        // Add runtime info to default context if provided
        config.runtimeInfo?.let { info ->
            putAll(info.toMap())
        }
    }

    return StructuredLogger(
        name = config.name,
        appenders = config.appenders.toList(),
        defaultContext = enrichedContext,
        contextScope = config.contextScope ?: DefaultLoggingContextScope
    )
}

/**
 * Registry for formatter and appender factories.
 * Infrastructure layer should register implementations here.
 */
object LoggerComponentRegistry {
    var jsonFormatterFactory: (() -> LogFormatter)? = null
    var plainTextFormatterFactory: (() -> LogFormatter)? = null
    var consoleAppenderFactory: ((LogFormatter) -> LogAppender)? = null
}

/**
 * Helper to create formatters using registered factories.
 */
private fun ConsoleAppenderConfiguration.JsonLogFormatter(): LogFormatter {
    return LoggerComponentRegistry.jsonFormatterFactory?.invoke()
        ?: throw IllegalStateException("JSON formatter factory not registered. Infrastructure module must register it.")
}

private fun ConsoleAppenderConfiguration.PlainTextLogFormatter(): LogFormatter {
    return LoggerComponentRegistry.plainTextFormatterFactory?.invoke()
        ?: throw IllegalStateException("Plain text formatter factory not registered. Infrastructure module must register it.")
}

private fun ConsoleAppenderConfiguration.ConsoleLogAppender(formatter: LogFormatter): LogAppender {
    return LoggerComponentRegistry.consoleAppenderFactory?.invoke(formatter)
        ?: throw IllegalStateException("Console appender factory not registered. Infrastructure module must register it.")
}

