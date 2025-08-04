package io.github.kamiazya.scopes.presentation.cli.utils

import io.github.kamiazya.scopes.application.error.ApplicationError

/**
 * Extension function to format ApplicationError into user-friendly messages.
 * Provides consistent error formatting across all CLI commands.
 */
fun ApplicationError.toUserMessage(): String =
    when (this) {
        is ApplicationError.Domain -> "Domain error: ${this.cause}"
        is ApplicationError.Repository -> "Repository error: ${this.cause}"
        is ApplicationError.UseCaseError.InvalidRequest ->
            "Invalid request: ${this.message}"
        is ApplicationError.UseCaseError.AuthorizationFailed ->
            "Authorization failed: ${this.reason}"
        is ApplicationError.UseCaseError.ConcurrencyConflict ->
            "Concurrency conflict: ${this.message}"
        is ApplicationError.IntegrationError.ServiceUnavailable ->
            "Service unavailable: ${this.serviceName}"
        is ApplicationError.IntegrationError.ServiceTimeout ->
            "Service timeout: ${this.serviceName} (${this.timeoutMs}ms)"
        is ApplicationError.IntegrationError.InvalidResponse ->
            "Invalid response: ${this.serviceName} - ${this.message}"
    }
