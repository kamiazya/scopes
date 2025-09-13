package io.github.kamiazya.scopes.devicesync.infrastructure.service

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.devicesync.application.port.EventQueryPort
import io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError
import io.github.kamiazya.scopes.devicesync.domain.repository.SynchronizationRepository
import io.github.kamiazya.scopes.devicesync.domain.service.DeviceSynchronizationService
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictResolution
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictResolutionStrategy
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictStatus
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictType
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.EventConflict
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ResolutionAction
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ResolvedConflict
import io.github.kamiazya.scopes.devicesync.domain.valueobject.SynchronizationResult
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Default implementation of DeviceSynchronizationService.
 */
class DefaultDeviceSynchronizationService(private val syncRepository: SynchronizationRepository, private val eventReader: EventQueryPort) :
    DeviceSynchronizationService {

    override suspend fun synchronize(remoteDeviceId: DeviceId, since: Instant?): Either<SynchronizationError, SynchronizationResult> =
        syncRepository.getSyncState(remoteDeviceId)
            .flatMap { syncState ->
                // Check if device can sync using domain logic
                if (!syncState.canSync()) {
                    return@flatMap Either.Left(
                        SynchronizationError.InvalidDeviceError(
                            deviceId = remoteDeviceId.value,
                            configurationIssue = SynchronizationError.ConfigurationIssue.MISSING_SYNC_CAPABILITY,
                        ),
                    )
                }

                // Start sync using domain logic
                val syncingState = syncState.startSync(now = Clock.System.now())
                syncRepository.updateSyncState(syncingState)
                    .flatMap {
                        // Get events to push
                        val pushSince = since ?: syncState.lastSuccessfulPush ?: Instant.DISTANT_PAST

                        eventReader.getEventsSince(
                            since = pushSince,
                            limit = 1000,
                        )
                            .mapLeft { error ->
                                SynchronizationError.NetworkError(
                                    deviceId = remoteDeviceId.value,
                                    errorType = SynchronizationError.NetworkErrorType.TIMEOUT,
                                    cause = null,
                                )
                            }
                            .flatMap { events ->
                                // Get local vector clock
                                syncRepository.getLocalVectorClock()
                                    .flatMap { localClock ->
                                        // Detect conflicts using domain logic
                                        val conflictStatus = detectConflict(localClock, syncState.remoteVectorClock)

                                        val conflicts = if (conflictStatus == ConflictStatus.CONFLICT || conflictStatus == ConflictStatus.CONCURRENT) {
                                            // TODO: Implement proper conflict detection using SyncConflict.detect()
                                            listOf(
                                                EventConflict(
                                                    eventId = "example-conflict",
                                                    localVersion = 1,
                                                    remoteVersion = 2,
                                                    conflictType = ConflictType.CONCURRENT_MODIFICATION,
                                                ),
                                            )
                                        } else {
                                            emptyList()
                                        }

                                        // Update vector clock using domain logic
                                        val newClock = localClock.merge(syncState.remoteVectorClock)
                                            .increment(remoteDeviceId)

                                        syncRepository.updateLocalVectorClock(newClock)
                                            .flatMap {
                                                // Mark sync as success using domain logic
                                                val successState = syncingState.markSyncSuccess(
                                                    eventsPushed = events.size,
                                                    eventsPulled = 0, // TODO: Implement pull logic
                                                    newRemoteVectorClock = newClock,
                                                    now = Clock.System.now(),
                                                )

                                                syncRepository.updateSyncState(successState)
                                                    .map {
                                                        SynchronizationResult(
                                                            deviceId = remoteDeviceId,
                                                            eventsPushed = events.size,
                                                            eventsPulled = 0, // Simplified - would need actual pull logic
                                                            conflicts = conflicts,
                                                            newVectorClock = newClock,
                                                            syncedAt = successState.lastSyncAt ?: Clock.System.now(),
                                                        )
                                                    }
                                                    .mapLeft { error ->
                                                        // Mark sync as failed on any error during the final update
                                                        syncingState.markSyncFailed("Failed to update sync state")
                                                            .let { failedState ->
                                                                syncRepository.updateSyncState(failedState)
                                                            }
                                                        error
                                                    }
                                            }
                                            .mapLeft { error ->
                                                // Mark sync as failed on any error during vector clock update
                                                syncingState.markSyncFailed("Failed to update vector clock")
                                                    .let { failedState ->
                                                        syncRepository.updateSyncState(failedState)
                                                    }
                                                error
                                            }
                                    }
                            }
                            .mapLeft { error ->
                                // Mark sync as failed on any error during event retrieval
                                syncingState.markSyncFailed("Failed to retrieve events")
                                    .let { failedState ->
                                        syncRepository.updateSyncState(failedState)
                                    }
                                error
                            }
                    }
            }

    override fun detectConflict(localClock: VectorClock, remoteClock: VectorClock): ConflictStatus = when {
        localClock == remoteClock -> ConflictStatus.NO_CONFLICT
        localClock.happenedBefore(remoteClock) -> ConflictStatus.NO_CONFLICT
        remoteClock.happenedBefore(localClock) -> ConflictStatus.NO_CONFLICT
        localClock.isConcurrentWith(remoteClock) -> ConflictStatus.CONCURRENT
        else -> ConflictStatus.CONFLICT
    }

    override suspend fun resolveConflicts(
        conflicts: List<EventConflict>,
        strategy: ConflictResolutionStrategy,
    ): Either<SynchronizationError, ConflictResolution> {
        val resolved = mutableListOf<ResolvedConflict>()
        val unresolved = mutableListOf<EventConflict>()

        conflicts.forEach { conflict ->
            when (strategy) {
                ConflictResolutionStrategy.LOCAL_WINS -> {
                    resolved.add(
                        ResolvedConflict(
                            conflict = conflict,
                            resolution = ResolutionAction.KEPT_LOCAL,
                        ),
                    )
                }
                ConflictResolutionStrategy.REMOTE_WINS -> {
                    resolved.add(
                        ResolvedConflict(
                            conflict = conflict,
                            resolution = ResolutionAction.ACCEPTED_REMOTE,
                        ),
                    )
                }
                ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                    // Simplified - would need timestamp comparison
                    resolved.add(
                        ResolvedConflict(
                            conflict = conflict,
                            resolution = ResolutionAction.KEPT_LOCAL,
                        ),
                    )
                }
                ConflictResolutionStrategy.MANUAL -> {
                    unresolved.add(conflict)
                }
                ConflictResolutionStrategy.MERGE -> {
                    // For now, MERGE is not implemented, defer to manual
                    unresolved.add(conflict)
                }
            }
        }

        return Either.Right(
            ConflictResolution(
                resolved = resolved,
                unresolved = unresolved,
                strategy = strategy,
            ),
        )
    }
}
