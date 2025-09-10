package io.github.kamiazya.scopes.collaborativeversioning.application.error

/**
 * Base class for all application layer errors in the collaborative versioning context.
 *
 * Application errors represent failures that occur during use case execution,
 * including validation failures, business rule violations, and infrastructure issues.
 */
abstract class ApplicationError {
    /**
     * Human-readable error message.
     */
    abstract val message: String

    override fun toString(): String = "${this::class.simpleName}: $message"
}
