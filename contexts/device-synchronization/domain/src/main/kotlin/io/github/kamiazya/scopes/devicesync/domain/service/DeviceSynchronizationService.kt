package io.github.kamiazya.scopes.devicesync.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictResolution
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictResolutionStrategy
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictStatus
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.EventConflict
import io.github.kamiazya.scopes.devicesync.domain.valueobject.SynchronizationResult
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import kotlinx.datetime.Instant

/**
 * Domain service contract for device synchronization.
 *
 * Implementations provide I/O-bound synchronization operations and
 * conflict detection/resolution strategies.
 */
interface DeviceSynchronizationService {
    /** Perform synchronization with a remote device. */
    suspend fun synchronize(remoteDeviceId: DeviceId, since: Instant?): Either<SynchronizationError, SynchronizationResult>

    /** Detect conflict status between two vector clocks. */
    fun detectConflict(localClock: VectorClock, remoteClock: VectorClock): ConflictStatus

    /** Resolve conflicts using the specified strategy. */
    suspend fun resolveConflicts(conflicts: List<EventConflict>, strategy: ConflictResolutionStrategy): Either<SynchronizationError, ConflictResolution>
}
