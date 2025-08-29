package io.github.kamiazya.scopes.devicesync.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.devicesync.domain.entity.SyncState
import io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock

/**
 * Repository for managing device synchronization state.
 */
interface SynchronizationRepository {

    /**
     * Gets the current synchronization state for a device.
     */
    suspend fun getSyncState(deviceId: DeviceId): Either<SynchronizationError, SyncState>

    /**
     * Updates the synchronization state for a device.
     */
    suspend fun updateSyncState(syncState: SyncState): Either<SynchronizationError, Unit>

    /**
     * Gets the local device's vector clock.
     */
    suspend fun getLocalVectorClock(): Either<SynchronizationError, VectorClock>

    /**
     * Updates the local device's vector clock.
     */
    suspend fun updateLocalVectorClock(vectorClock: VectorClock): Either<SynchronizationError, Unit>

    /**
     * Lists all known devices.
     */
    suspend fun listKnownDevices(): Either<SynchronizationError, List<DeviceId>>

    /**
     * Registers a new device for synchronization.
     */
    suspend fun registerDevice(deviceId: DeviceId): Either<SynchronizationError, Unit>

    /**
     * Unregisters a device from synchronization.
     */
    suspend fun unregisterDevice(deviceId: DeviceId): Either<SynchronizationError, Unit>
}
