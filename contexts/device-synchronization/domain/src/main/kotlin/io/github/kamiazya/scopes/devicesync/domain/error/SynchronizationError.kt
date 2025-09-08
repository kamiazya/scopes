package io.github.kamiazya.scopes.devicesync.domain.error

import kotlinx.datetime.Instant

/**
 * Domain errors that can occur during device synchronization operations.
 */
sealed class SynchronizationError {
    abstract val occurredAt: Instant

    /**
     * Network error occurred during synchronization.
     */
    data class NetworkError(val deviceId: String, val errorType: NetworkErrorType, override val occurredAt: Instant, val cause: Throwable? = null) :
        SynchronizationError()

    enum class NetworkErrorType {
        CONNECTION_REFUSED,
        TIMEOUT,
        DNS_RESOLUTION_FAILED,
        SSL_HANDSHAKE_FAILED,
        NO_ROUTE_TO_HOST,
    }

    /**
     * The remote device is unreachable.
     */
    data class DeviceUnreachableError(val deviceId: String, val lastKnownStatus: String? = null, val attemptCount: Int = 1, override val occurredAt: Instant) :
        SynchronizationError()

    /**
     * Version conflict detected between devices.
     */
    data class VersionConflictError(
        val localVersion: Long,
        val remoteVersion: Long,
        val aggregateId: String,
        val conflictType: ConflictType = ConflictType.VERSION_MISMATCH,
        override val occurredAt: Instant,
    ) : SynchronizationError()

    enum class ConflictType {
        VERSION_MISMATCH,
        CONCURRENT_UPDATE,
        DELETED_ON_REMOTE,
        DELETED_ON_LOCAL,
    }

    /**
     * Failed to resolve conflicts during synchronization.
     */
    data class ConflictResolutionError(
        val unresolvedCount: Int,
        val resolvedCount: Int,
        val resolutionStrategy: String,
        val failureReason: ResolutionFailureReason,
        override val occurredAt: Instant,
    ) : SynchronizationError()

    enum class ResolutionFailureReason {
        TOO_MANY_CONFLICTS,
        STRATEGY_NOT_APPLICABLE,
        DATA_LOSS_DETECTED,
        USER_INTERVENTION_REQUIRED,
        TIMEOUT,
    }

    /**
     * Synchronization protocol error.
     */
    data class ProtocolError(
        val protocolVersion: String,
        val expectedFormat: String? = null,
        val actualFormat: String? = null,
        val operation: String,
        override val occurredAt: Instant,
    ) : SynchronizationError()

    /**
     * Invalid device configuration.
     */
    data class InvalidDeviceError(
        val deviceId: String,
        val configurationIssue: ConfigurationIssue,
        val requiredConfiguration: String? = null,
        override val occurredAt: Instant,
    ) : SynchronizationError()

    enum class ConfigurationIssue {
        MISSING_DEVICE_ID,
        INVALID_DEVICE_ID,
        MISSING_SYNC_CAPABILITY,
        OUTDATED_PROTOCOL,
        UNAUTHORIZED,
    }
}
