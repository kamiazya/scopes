package io.github.kamiazya.scopes.devicesync.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import kotlinx.datetime.Instant

/**
 * Domain service for device synchronization operations.
 *
 * This service handles the core logic of multi-device synchronization,
 * including conflict detection and resolution strategies.
 */
interface DeviceSynchronizationService {

    /**
     * Performs synchronization with a remote device.
     *
     * @param remoteDeviceId The device to synchronize with
     * @param since Optional timestamp to sync events after
     * @return Either an error or the synchronization result
     */
    suspend fun synchronize(remoteDeviceId: DeviceId, since: Instant? = null): Either<SynchronizationError, SynchronizationResult>

    /**
     * Determines if two vector clocks indicate a conflict.
     */
    fun detectConflict(localClock: VectorClock, remoteClock: VectorClock): ConflictStatus

    /**
     * Resolves conflicts between local and remote states.
     */
    suspend fun resolveConflicts(conflicts: List<EventConflict>, strategy: ConflictResolutionStrategy): Either<SynchronizationError, ConflictResolution>
}

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
    /** Keep local version */
    LOCAL_WINS,

    /** Keep remote version */
    REMOTE_WINS,

    /** Use latest timestamp */
    LAST_WRITE_WINS,

    /** Manual resolution required */
    MANUAL,
}

/**
 * Result of conflict resolution.
 */
data class ConflictResolution(val resolved: List<ResolvedConflict>, val unresolved: List<EventConflict>, val strategy: ConflictResolutionStrategy)

/**
 * A conflict that has been resolved.
 */
data class ResolvedConflict(val conflict: EventConflict, val resolution: ResolutionAction)

/**
 * Action taken to resolve a conflict.
 */
enum class ResolutionAction {
    KEPT_LOCAL,
    KEPT_REMOTE,
    MERGED,
    DEFERRED,
}
