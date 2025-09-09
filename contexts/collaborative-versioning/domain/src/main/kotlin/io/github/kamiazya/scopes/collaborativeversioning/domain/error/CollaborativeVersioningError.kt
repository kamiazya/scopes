package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import kotlinx.datetime.Instant

/**
 * Base error class for all collaborative versioning domain errors.
 */
sealed class CollaborativeVersioningError {
    abstract val occurredAt: Instant
}
