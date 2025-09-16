package io.github.kamiazya.scopes.platform.application.error

/**
 * Base marker interface for all application layer errors.
 *
 * Application errors represent failures that occur in the application layer,
 * such as validation errors, handler errors, or integration errors.
 *
 * Note:
 * - Do not expose Throwable or presentation-layer messages here.
 * - Propagate only structured, type-safe data (sealed classes/enums/value objects).
 */
interface ApplicationError
