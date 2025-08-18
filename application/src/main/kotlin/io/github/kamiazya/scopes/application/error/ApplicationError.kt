package io.github.kamiazya.scopes.application.error

/**
 * Type-safe structured error information for presentation layer.
 * This sealed class hierarchy provides compile-time safety for error handling,
 * containing only structured data without presentation-specific messages.
 * The presentation layer is responsible for generating user-facing messages.
 */
sealed class ApplicationError(
    open val recoverable: Boolean = true
)