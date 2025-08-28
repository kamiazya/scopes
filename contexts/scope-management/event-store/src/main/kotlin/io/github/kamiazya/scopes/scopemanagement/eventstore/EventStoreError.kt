package io.github.kamiazya.scopes.scopemanagement.eventstore

import kotlinx.datetime.Instant

/**
 * Errors that can occur during event store operations.
 */
sealed class EventStoreError : Exception() {
    abstract val occurredAt: Instant

    /**
     * Database connection or operation failed.
     */
    data class DatabaseError(override val occurredAt: Instant, val operation: String, override val cause: Throwable? = null) : EventStoreError()

    /**
     * Event data corruption detected.
     */
    data class CorruptedEvent(override val occurredAt: Instant, val eventId: String, val reason: String) : EventStoreError()

    /**
     * Vector clock conflict that cannot be automatically resolved.
     */
    data class VectorClockConflict(override val occurredAt: Instant, val localDeviceId: String, val remoteDeviceId: String, val conflictingTimestamp: Instant) :
        EventStoreError()

    /**
     * Device not found in the system.
     */
    data class DeviceNotFound(override val occurredAt: Instant, val deviceId: String) : EventStoreError()

    /**
     * Invalid event sequence detected.
     */
    data class InvalidEventSequence(override val occurredAt: Instant, val expectedSequence: Long, val actualSequence: Long, val deviceId: String) :
        EventStoreError()

    /**
     * Storage capacity exceeded.
     */
    data class StorageCapacityExceeded(override val occurredAt: Instant, val currentSize: Long, val maxSize: Long) : EventStoreError()
}
