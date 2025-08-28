package io.github.kamiazya.scopes.scopemanagement.eventstore.sync

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.eventstore.dto.ConflictResolutionResult
import io.github.kamiazya.scopes.scopemanagement.eventstore.dto.ConflictResolutionStrategy
import io.github.kamiazya.scopes.scopemanagement.eventstore.dto.PullResult
import io.github.kamiazya.scopes.scopemanagement.eventstore.dto.PushResult
import io.github.kamiazya.scopes.scopemanagement.eventstore.dto.SynchronizationResult
import io.github.kamiazya.scopes.scopemanagement.eventstore.model.StoredEvent
import io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject.DeviceId
import kotlinx.datetime.Instant

/**
 * Service for synchronizing events between devices.
 *
 * Handles push and pull synchronization with conflict detection and resolution.
 */
interface SynchronizationService {

    /**
     * Pushes local events to a remote device.
     *
     * @param remoteDeviceId The target device ID
     * @param since Timestamp to push events after (null for all events)
     * @return Either an error or the result of the push operation
     */
    suspend fun pushEvents(remoteDeviceId: DeviceId, since: Instant? = null): Either<SynchronizationError, PushResult>

    /**
     * Pulls events from a remote device.
     *
     * @param remoteDeviceId The source device ID
     * @param since Timestamp to pull events after (null for all events)
     * @return Either an error or the result of the pull operation
     */
    suspend fun pullEvents(remoteDeviceId: DeviceId, since: Instant? = null): Either<SynchronizationError, PullResult>

    /**
     * Performs a full synchronization with a remote device.
     * This includes both push and pull operations with conflict resolution.
     *
     * @param remoteDeviceId The device to synchronize with
     * @return Either an error or the synchronization result
     */
    suspend fun synchronize(remoteDeviceId: DeviceId): Either<SynchronizationError, SynchronizationResult>

    /**
     * Resolves conflicts between local and remote events.
     *
     * @param conflicts List of conflicting event pairs (local, remote)
     * @param strategy The resolution strategy to use
     * @return Either an error or the resolution result
     */
    suspend fun resolveConflicts(
        conflicts: List<Pair<StoredEvent, StoredEvent>>,
        strategy: ConflictResolutionStrategy,
    ): Either<SynchronizationError, ConflictResolutionResult>
}
