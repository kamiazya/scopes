package io.github.kamiazya.scopes.application.test

import io.github.kamiazya.scopes.application.logging.LogLevel
import io.github.kamiazya.scopes.application.logging.Logger

/**
 * A no-op logger implementation for testing.
 */
class MockLogger : Logger {
    override fun debug(message: String, context: Map<String, Any>) {
        // No-op for testing
    }

    override fun info(message: String, context: Map<String, Any>) {
        // No-op for testing
    }

    override fun warn(message: String, context: Map<String, Any>) {
        // No-op for testing
    }

    override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {
        // No-op for testing
    }

    override fun withContext(context: Map<String, Any>): Logger = this

    override fun withName(name: String): Logger = this

    override fun isEnabledFor(level: LogLevel): Boolean = true
}
