package io.github.kamiazya.scopes.infrastructure.logging

import io.github.kamiazya.scopes.application.logging.LoggerComponentRegistry
import io.github.kamiazya.scopes.infrastructure.logging.appender.ConsoleLogAppender
import io.github.kamiazya.scopes.infrastructure.logging.formatter.JsonLogFormatter
import io.github.kamiazya.scopes.infrastructure.logging.formatter.PlainTextLogFormatter

/**
 * Initializes the logger component registry with infrastructure implementations.
 * This must be called during application startup.
 */
object LoggerComponentInitializer {

    /**
     * Registers all logger component factories.
     */
    fun initialize() {
        LoggerComponentRegistry.jsonFormatterFactory = { JsonLogFormatter() }
        LoggerComponentRegistry.plainTextFormatterFactory = { PlainTextLogFormatter() }
        LoggerComponentRegistry.consoleAppenderFactory = { formatter -> ConsoleLogAppender(formatter) }
    }
}
