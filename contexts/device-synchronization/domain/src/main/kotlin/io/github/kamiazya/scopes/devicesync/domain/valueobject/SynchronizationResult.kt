package io.github.kamiazya.scopes.devicesync.domain.valueobject

import kotlinx.datetime.Instant

/**
 * Result of a synchronization operation.
 */
data class SynchronizationResult(
    val deviceId: DeviceId,
    val eventsPushed: Int,
    val eventsPulled: Int,
    val conflicts: List<EventConflict>,
    val newVectorClock: VectorClock,
    val syncedAt: Instant,
)

/**
 * Represents a conflict between events.
 */
data class EventConflict(val eventId: String, val localVersion: Long, val remoteVersion: Long, val conflictType: ConflictType)

/**
 * Types of conflicts that can occur during synchronization.
 */
enum class ConflictType {
    /** Same aggregate modified on different devices */
    CONCURRENT_MODIFICATION,

    /** Version numbers don't match expected sequence */
    VERSION_MISMATCH,

    /** Event exists on one device but not the other */
    MISSING_DEPENDENCY,

    /** Entity deleted on one device but modified on another */
    DELETED_MODIFIED,

    /** Schema or format differences */
    SCHEMA_MISMATCH,
}

/**
 * Status of conflict detection.
 */
enum class ConflictStatus {
    /** No conflicts detected */
    NO_CONFLICT,

    /** Events are concurrent (potential conflict) */
    CONCURRENT,

    /** Direct conflict detected */
    CONFLICT,
}

/**
 * Strategy for resolving conflicts.
 */
enum class ConflictResolutionStrategy {
    /** Most recent change wins */
    LAST_WRITE_WINS,

    /** Manual resolution required */
    MANUAL,

    /** Merge changes if possible */
    MERGE,

    /** Always prefer local changes */
    LOCAL_WINS,

    /** Always prefer remote changes */
    REMOTE_WINS,
}

/**
 * Result of conflict resolution.
 */
data class ConflictResolution(val resolved: List<ResolvedConflict>, val unresolved: List<EventConflict>, val strategy: ConflictResolutionStrategy)

/**
 * Represents a resolved conflict.
 */
data class ResolvedConflict(val conflict: EventConflict, val resolution: ResolutionAction)

/**
 * Actions taken to resolve a conflict.
 */
enum class ResolutionAction {
    /** Kept local version */
    KEPT_LOCAL,

    /** Accepted remote version */
    ACCEPTED_REMOTE,

    /** Merged both versions */
    MERGED,

    /** Created new version incorporating both changes */
    CREATED_NEW,

    /** Deferred for manual resolution */
    DEFERRED,
}
