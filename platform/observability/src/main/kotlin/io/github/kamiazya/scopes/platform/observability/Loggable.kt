package io.github.kamiazya.scopes.platform.observability

import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Trait that provides logging capabilities to classes.
 *
 * Classes implementing this trait get access to a logger instance
 * that uses the class name as the logger name.
 */
interface Loggable {
    /**
     * Logger instance for this class.
     * Uses the implementing class name as the logger name.
     */
    val logger: Logger
        get() = ConsoleLogger(this::class.simpleName ?: this::class.qualifiedName ?: "AnonymousClass")
}
