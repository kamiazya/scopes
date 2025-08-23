package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Base type for all errors in the Scopes domain.
 * This is a simplified version focused on scope management context.
 */
sealed class ScopesError {
    /**
     * Contextual timestamp when the error occurred.
     */
    abstract val occurredAt: Instant
}
