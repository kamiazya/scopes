package io.github.kamiazya.scopes.scopemanagement.eventstore.sync

import kotlinx.datetime.Instant

/**
 * Errors that can occur during synchronization operations.
 */
sealed class SynchronizationError : Exception() {
    abstract val occurredAt: Instant

    /**
     * Remote device is not reachable.
     */
    data class RemoteDeviceUnreachable(override val occurredAt: Instant, val deviceId: String, val reason: String) : SynchronizationError()

    /**
     * Authentication failed with remote device.
     */
    data class AuthenticationFailed(override val occurredAt: Instant, val deviceId: String) : SynchronizationError()

    /**
     * Network error during synchronization.
     */
    data class NetworkError(override val occurredAt: Instant, val operation: String, override val cause: Throwable? = null) : SynchronizationError()

    /**
     * Version mismatch between devices.
     */
    data class ProtocolVersionMismatch(override val occurredAt: Instant, val localVersion: String, val remoteVersion: String) : SynchronizationError()

    /**
     * Conflict resolution failed.
     */
    data class ConflictResolutionFailed(override val occurredAt: Instant, val conflictCount: Int, val reason: String) : SynchronizationError()

    /**
     * Synchronization was cancelled.
     */
    data class SynchronizationCancelled(override val occurredAt: Instant, val reason: String) : SynchronizationError()

    /**
     * Invalid synchronization request.
     */
    data class InvalidRequest(override val occurredAt: Instant, val reason: String) : SynchronizationError()
}
