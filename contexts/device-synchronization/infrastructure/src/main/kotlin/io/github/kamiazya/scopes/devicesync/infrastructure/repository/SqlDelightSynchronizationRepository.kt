package io.github.kamiazya.scopes.devicesync.infrastructure.repository

import arrow.core.Either
import io.github.kamiazya.scopes.devicesync.db.DeviceQueries
import io.github.kamiazya.scopes.devicesync.db.VectorClockQueries
import io.github.kamiazya.scopes.devicesync.domain.entity.SyncState
import io.github.kamiazya.scopes.devicesync.domain.entity.SyncStatus
import io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError
import io.github.kamiazya.scopes.devicesync.domain.repository.SynchronizationRepository
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import io.github.kamiazya.scopes.platform.commons.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SQLDelight implementation of SynchronizationRepository.
 */
class SqlDelightSynchronizationRepository(
    private val deviceQueries: DeviceQueries,
    private val vectorClockQueries: VectorClockQueries,
    private val timeProvider: TimeProvider,
) : SynchronizationRepository {

    override suspend fun getSyncState(deviceId: DeviceId): Either<SynchronizationError, SyncState> = withContext(Dispatchers.IO) {
        try {
            val device = deviceQueries.findDeviceById(deviceId.value).executeAsOneOrNull()
                ?: return@withContext Either.Left(
                    SynchronizationError.InvalidDeviceError(
                        deviceId = deviceId.value,
                        configurationIssue = SynchronizationError.ConfigurationIssue.INVALID_DEVICE_ID,
                    ),
                )

            val vectorClock = getVectorClockForDevice(deviceId.value)

            Either.Right(
                SyncState(
                    deviceId = deviceId,
                    lastSyncAt = device.last_sync_at?.let { Instant.fromEpochMilliseconds(it) },
                    remoteVectorClock = vectorClock,
                    lastSuccessfulPush = device.last_successful_push?.let { Instant.fromEpochMilliseconds(it) },
                    lastSuccessfulPull = device.last_successful_pull?.let { Instant.fromEpochMilliseconds(it) },
                    syncStatus = SyncStatus.valueOf(device.sync_status),
                    pendingChanges = device.pending_changes.toInt(),
                ),
            )
        } catch (e: Exception) {
            Either.Left(
                SynchronizationError.NetworkError(
                    deviceId = deviceId.value,
                    errorType = SynchronizationError.NetworkErrorType.TIMEOUT,
                    cause = e,
                ),
            )
        }
    }

    override suspend fun updateSyncState(syncState: SyncState): Either<SynchronizationError, Unit> = withContext(Dispatchers.IO) {
        try {
            deviceQueries.updateSyncState(
                last_sync_at = syncState.lastSyncAt?.toEpochMilliseconds(),
                last_successful_push = syncState.lastSuccessfulPush?.toEpochMilliseconds(),
                last_successful_pull = syncState.lastSuccessfulPull?.toEpochMilliseconds(),
                sync_status = syncState.syncStatus.name,
                pending_changes = syncState.pendingChanges.toLong(),
                updated_at = timeProvider.now().toEpochMilliseconds(),
                device_id = syncState.deviceId.value,
            )

            // Update vector clock
            updateVectorClockForDevice(syncState.deviceId.value, syncState.remoteVectorClock)

            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(
                SynchronizationError.NetworkError(
                    deviceId = syncState.deviceId.value,
                    errorType = SynchronizationError.NetworkErrorType.TIMEOUT,
                    cause = e,
                ),
            )
        }
    }

    override suspend fun getLocalVectorClock(): Either<SynchronizationError, VectorClock> = withContext(Dispatchers.IO) {
        try {
            val clocks = vectorClockQueries.getVectorClock("LOCAL")
                .executeAsList()
                .associate { it.component_device to it.timestamp }
            Either.Right(VectorClock(clocks))
        } catch (e: Exception) {
            Either.Left(
                SynchronizationError.NetworkError(
                    deviceId = "LOCAL",
                    errorType = SynchronizationError.NetworkErrorType.TIMEOUT,
                    cause = e,
                ),
            )
        }
    }

    override suspend fun updateLocalVectorClock(vectorClock: VectorClock): Either<SynchronizationError, Unit> = withContext(Dispatchers.IO) {
        try {
            updateVectorClockForDevice("LOCAL", vectorClock)
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(
                SynchronizationError.NetworkError(
                    deviceId = "LOCAL",
                    errorType = SynchronizationError.NetworkErrorType.TIMEOUT,
                    cause = e,
                ),
            )
        }
    }

    override suspend fun listKnownDevices(): Either<SynchronizationError, List<DeviceId>> = withContext(Dispatchers.IO) {
        try {
            val devices = deviceQueries.listDevices()
                .executeAsList()
                .map { DeviceId(it) }
            Either.Right(devices)
        } catch (e: Exception) {
            Either.Left(
                SynchronizationError.NetworkError(
                    deviceId = "LOCAL",
                    errorType = SynchronizationError.NetworkErrorType.TIMEOUT,
                    cause = e,
                ),
            )
        }
    }

    override suspend fun registerDevice(deviceId: DeviceId): Either<SynchronizationError, Unit> = withContext(Dispatchers.IO) {
        try {
            val now = timeProvider.now().toEpochMilliseconds()
            deviceQueries.upsertDevice(
                device_id = deviceId.value,
                last_sync_at = null,
                last_successful_push = null,
                last_successful_pull = null,
                sync_status = SyncStatus.NEVER_SYNCED.name,
                pending_changes = 0,
                created_at = now,
                updated_at = now,
            )
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(
                SynchronizationError.InvalidDeviceError(
                    deviceId = deviceId.value,
                    configurationIssue = SynchronizationError.ConfigurationIssue.INVALID_DEVICE_ID,
                ),
            )
        }
    }

    override suspend fun unregisterDevice(deviceId: DeviceId): Either<SynchronizationError, Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete vector clock entries
            vectorClockQueries.deleteVectorClock(deviceId.value)

            // Delete device entry
            deviceQueries.deleteDevice(deviceId.value)

            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(
                SynchronizationError.InvalidDeviceError(
                    deviceId = deviceId.value,
                    configurationIssue = SynchronizationError.ConfigurationIssue.INVALID_DEVICE_ID,
                ),
            )
        }
    }

    private fun getVectorClockForDevice(deviceId: String): VectorClock {
        val clocks = vectorClockQueries.getVectorClock(deviceId)
            .executeAsList()
            .associate { it.component_device to it.timestamp }
        return VectorClock(clocks)
    }

    private fun updateVectorClockForDevice(deviceId: String, vectorClock: VectorClock) {
        // Use upsert (INSERT OR REPLACE) for atomic updates
        // This ensures we don't have a window where the vector clock is deleted
        vectorClock.clocks.forEach { (componentDevice, timestamp) ->
            vectorClockQueries.upsertClockComponent(
                device_id = deviceId,
                component_device = componentDevice,
                timestamp = timestamp,
            )
        }

        // Remove any components that are no longer in the vector clock
        // Get current components from DB
        val currentComponents = vectorClockQueries.getVectorClock(deviceId)
            .executeAsList()
            .map { it.component_device }
            .toSet()

        // Remove components not in the new vector clock
        val newComponents = vectorClock.clocks.keys
        currentComponents.subtract(newComponents).forEach { componentToRemove ->
            vectorClockQueries.deleteClockComponent(deviceId, componentToRemove)
        }
    }
}
