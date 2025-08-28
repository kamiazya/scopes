package io.github.kamiazya.scopes.scopemanagement.eventstore

import arrow.core.Either
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.eventstore.model.StoredEvent
import io.github.kamiazya.scopes.scopemanagement.eventstore.model.VectorClock
import io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject.DeviceId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Interface for event storage and retrieval with multi-device synchronization support.
 *
 * This interface provides methods for storing and retrieving domain events with
 * device-specific metadata and vector clocks for conflict detection.
 */
interface EventStore {

    /**
     * Stores a domain event with device metadata.
     *
     * @param event The domain event to store
     * @param deviceId The ID of the device storing the event
     * @param vectorClock The vector clock at the time of the event
     * @return Either an error or the stored event with its assigned sequence number
     */
    suspend fun store(event: DomainEvent, deviceId: DeviceId, vectorClock: VectorClock): Either<EventStoreError, StoredEvent>

    /**
     * Retrieves events since a specific timestamp.
     *
     * @param since The timestamp to retrieve events after (exclusive)
     * @param deviceId Optional device ID to filter events by
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsSince(since: Instant, deviceId: DeviceId? = null): Either<EventStoreError, List<StoredEvent>>

    /**
     * Retrieves events by aggregate ID.
     *
     * @param aggregateId The aggregate ID to filter events by
     * @param since Optional timestamp to retrieve events after
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsByAggregate(aggregateId: String, since: Instant? = null): Either<EventStoreError, List<StoredEvent>>

    /**
     * Retrieves the current vector clock for a device.
     *
     * @param deviceId The device ID to get the vector clock for
     * @return Either an error or the current vector clock
     */
    suspend fun getCurrentVectorClock(deviceId: DeviceId): Either<EventStoreError, VectorClock>

    /**
     * Updates the vector clock for a device after receiving events from another device.
     *
     * @param deviceId The local device ID
     * @param remoteVectorClock The vector clock from the remote device
     * @return Either an error or the updated vector clock
     */
    suspend fun updateVectorClock(deviceId: DeviceId, remoteVectorClock: VectorClock): Either<EventStoreError, VectorClock>

    /**
     * Streams all events as they are stored.
     *
     * @return A Flow of stored events
     */
    fun streamEvents(): Flow<StoredEvent>

    /**
     * Checks if there are potential conflicts between local and remote vector clocks.
     *
     * @param localClock The local vector clock
     * @param remoteClock The remote vector clock
     * @return true if there are potential conflicts, false otherwise
     */
    fun hasConflicts(localClock: VectorClock, remoteClock: VectorClock): Boolean

    /**
     * Finds events that are in conflict based on vector clocks.
     *
     * @param localDeviceId The local device ID
     * @param remoteEvents Events from a remote device
     * @return Either an error or a list of conflicting event pairs (local, remote)
     */
    suspend fun findConflictingEvents(localDeviceId: DeviceId, remoteEvents: List<StoredEvent>): Either<EventStoreError, List<Pair<StoredEvent, StoredEvent>>>
}
