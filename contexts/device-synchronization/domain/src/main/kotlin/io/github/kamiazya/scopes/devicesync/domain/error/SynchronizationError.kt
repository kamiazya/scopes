package io.github.kamiazya.scopes.devicesync.domain.error

/**
 * Domain errors that can occur during device synchronization operations.
 */
sealed class SynchronizationError {

    /**
     * Network error occurred during synchronization.
     */
    data class NetworkError(val deviceId: String, val errorType: NetworkErrorType, val cause: Throwable? = null) : SynchronizationError()

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
    data class DeviceUnreachableError(val deviceId: String, val lastKnownStatus: String? = null, val attemptCount: Int = 1) : SynchronizationError()

    /**
     * Version conflict detected between devices.
     */
    data class VersionConflictError(
        val localVersion: Long,
        val remoteVersion: Long,
        val aggregateId: String,
        val conflictType: ConflictType = ConflictType.VERSION_MISMATCH,
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
    data class ProtocolError(val protocolVersion: String, val expectedFormat: String? = null, val actualFormat: String? = null, val operation: String) :
        SynchronizationError()

    /**
     * Invalid device configuration.
     */
    data class InvalidDeviceError(val deviceId: String, val configurationIssue: ConfigurationIssue, val requiredConfiguration: String? = null) :
        SynchronizationError()

    enum class ConfigurationIssue {
        MISSING_DEVICE_ID,
        INVALID_DEVICE_ID,
        MISSING_SYNC_CAPABILITY,
        OUTDATED_PROTOCOL,
        UNAUTHORIZED,
    }
}
