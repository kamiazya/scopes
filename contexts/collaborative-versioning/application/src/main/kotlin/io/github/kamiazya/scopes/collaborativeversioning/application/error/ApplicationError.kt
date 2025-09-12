package io.github.kamiazya.scopes.collaborativeversioning.application.error

/**
 * Type-safe structured error information for presentation layer.
 * This sealed class hierarchy provides compile-time safety for error handling,
 * containing only structured data without presentation-specific messages.
 * The presentation layer is responsible for generating user-facing messages.
 *
 * Following Clean Architecture principles, application errors should not
 * contain infrastructure concerns like timestamps or exception details.
 */
sealed class ApplicationError(open val recoverable: Boolean = true)
