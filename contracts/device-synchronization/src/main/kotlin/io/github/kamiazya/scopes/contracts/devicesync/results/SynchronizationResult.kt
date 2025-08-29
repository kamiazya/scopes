package io.github.kamiazya.scopes.contracts.devicesync.results

import kotlinx.datetime.Instant

/**
 * Result of device synchronization.
 */
public data class SynchronizationResult(val eventsPushed: Int, val eventsPulled: Int, val conflicts: List<ConflictResult>, val synchronizedAt: Instant)

/**
 * Result of conflict resolution.
 */
public data class ConflictResult(val eventId: String, val resolution: ConflictResolution, val reason: String? = null)

/**
 * How a conflict was resolved.
 */
public enum class ConflictResolution {
    KEPT_LOCAL,
    KEPT_REMOTE,
    MERGED,
    MANUAL_REVIEW_REQUIRED,
}
