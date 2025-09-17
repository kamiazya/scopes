package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors specific to aggregate version operations.
 *
 * These errors represent violations of version constraints:
 * - Negative version numbers
 * - Version overflow
 * - Invalid version transitions
 */
sealed class AggregateVersionError : ScopesError() {

    /**
     * Attempted to create a version with a negative value.
     */

    /**
     * Version number exceeds maximum allowed value.
     */

    /**
     * Invalid version transition detected.
     * Version must increment by exactly 1.
     */
}
