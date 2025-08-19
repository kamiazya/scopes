package io.github.kamiazya.scopes.domain.error

import kotlinx.datetime.Instant

/**
 * Errors specific to aggregate version operations.
 * 
 * These errors represent violations of version constraints:
 * - Negative version numbers
 * - Version overflow
 * - Invalid version transitions
 */
sealed class AggregateVersionError : ConceptualModelError() {
    
    /**
     * Attempted to create a version with a negative value.
     */
    data class NegativeVersion(
        override val occurredAt: Instant,
        val attemptedVersion: Int
    ) : AggregateVersionError()
    
    /**
     * Version number exceeds maximum allowed value.
     */
    data class VersionOverflow(
        override val occurredAt: Instant,
        val currentVersion: Int,
        val maxVersion: Int
    ) : AggregateVersionError()
    
    /**
     * Invalid version transition detected.
     * Version must increment by exactly 1.
     */
    data class InvalidVersionTransition(
        override val occurredAt: Instant,
        val currentVersion: Int,
        val attemptedVersion: Int
    ) : AggregateVersionError()
}