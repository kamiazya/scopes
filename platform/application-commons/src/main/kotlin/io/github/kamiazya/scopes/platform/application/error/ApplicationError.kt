package io.github.kamiazya.scopes.platform.application.error

import kotlinx.datetime.Instant

/**
 * Base interface for all application layer errors.
 *
 * Application errors represent failures that occur in the application layer,
 * such as validation errors, handler errors, or integration errors.
 */
interface ApplicationError {
    val occurredAt: Instant
    val cause: Throwable?
}
