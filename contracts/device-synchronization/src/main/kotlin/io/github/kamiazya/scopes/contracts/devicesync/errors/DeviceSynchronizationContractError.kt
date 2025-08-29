package io.github.kamiazya.scopes.contracts.devicesync.errors

import io.github.kamiazya.scopes.contracts.devicesync.types.DeviceStatus
import io.github.kamiazya.scopes.contracts.devicesync.types.SyncStatus
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Sealed interface representing all possible errors in the Device Synchronization contract layer.
 * These errors provide a stable API contract between bounded contexts with rich,
 * structured information for proper error handling.
 *
 * Key design principles:
 * - Rich error information without losing domain knowledge
 * - Structured data instead of plain strings
 * - Type-safe error handling for clients
 * - Clear categorization of error types
 */
public sealed interface DeviceSynchronizationContractError {

    /**
     * Specific device registration failures.
     */
    public sealed interface RegistrationFailureType {
        /**
         * Device ID already exists in the system.
         * @property deviceId The duplicate device ID
         * @property registeredAt When the existing device was registered
         */
        public data class DuplicateDeviceId(public val deviceId: String, public val registeredAt: Instant? = null) : RegistrationFailureType

        /**
         * Device name already exists for user.
         * @property deviceName The duplicate device name
         * @property existingDeviceId The ID of the existing device
         */
        public data class DuplicateDeviceName(public val deviceName: String, public val existingDeviceId: String) : RegistrationFailureType

        /**
         * Invalid device name format.
         * @property deviceName The invalid device name
         * @property reason Specific reason for invalidity
         */
        public data class InvalidDeviceName(public val deviceName: String, public val reason: DeviceNameValidationFailure) : RegistrationFailureType

        /**
         * Maximum device limit reached.
         * @property currentCount Current number of devices
         * @property maximumAllowed Maximum allowed devices
         */
        public data class DeviceLimitExceeded(public val currentCount: Int, public val maximumAllowed: Int) : RegistrationFailureType
    }

    /**
     * Specific validation failures for device name.
     */
    public sealed interface DeviceNameValidationFailure {
        public data object Empty : DeviceNameValidationFailure
        public data class TooShort(public val minimumLength: Int, public val actualLength: Int) : DeviceNameValidationFailure
        public data class TooLong(public val maximumLength: Int, public val actualLength: Int) : DeviceNameValidationFailure
        public data class InvalidCharacters(public val prohibitedCharacters: List<Char>) : DeviceNameValidationFailure
    }

    /**
     * Specific synchronization failure types.
     */
    public sealed interface SynchronizationFailureType {
        /**
         * Network connectivity issue.
         * @property localDeviceId The local device attempting sync
         * @property remoteDeviceId The remote device being contacted
         * @property lastSuccessfulSync Last time sync succeeded
         */
        public data class NetworkFailure(public val localDeviceId: String, public val remoteDeviceId: String, public val lastSuccessfulSync: Instant? = null) :
            SynchronizationFailureType

        /**
         * Authentication failure between devices.
         * @property localDeviceId The local device
         * @property remoteDeviceId The remote device
         * @property reason Authentication failure reason
         */
        public data class AuthenticationFailure(public val localDeviceId: String, public val remoteDeviceId: String, public val reason: AuthFailureReason) :
            SynchronizationFailureType

        /**
         * Protocol version mismatch.
         * @property localVersion Local protocol version
         * @property remoteVersion Remote protocol version
         * @property minimumRequiredVersion Minimum version required
         */
        public data class ProtocolMismatch(public val localVersion: String, public val remoteVersion: String, public val minimumRequiredVersion: String) :
            SynchronizationFailureType

        /**
         * Data corruption detected.
         * @property localDeviceId The local device
         * @property remoteDeviceId The remote device
         * @property corruptedEventIds IDs of corrupted events
         */
        public data class DataCorruption(public val localDeviceId: String, public val remoteDeviceId: String, public val corruptedEventIds: List<String>) :
            SynchronizationFailureType
    }

    /**
     * Authentication failure reasons.
     */
    public sealed interface AuthFailureReason {
        public data object InvalidCredentials : AuthFailureReason
        public data object ExpiredToken : AuthFailureReason
        public data object DeviceNotAuthorized : AuthFailureReason
        public data object DeviceRevoked : AuthFailureReason
    }

    /**
     * Conflict resolution failure types.
     */
    public sealed interface ConflictResolutionFailureType {
        /**
         * Too many conflicts to resolve.
         * @property conflictCount Number of conflicts
         * @property maximumResolvable Maximum conflicts that can be resolved
         * @property affectedAggregateIds Aggregate IDs with conflicts
         */
        public data class TooManyConflicts(public val conflictCount: Int, public val maximumResolvable: Int, public val affectedAggregateIds: List<String>) :
            ConflictResolutionFailureType

        /**
         * Conflict resolution timeout.
         * @property conflictCount Number of conflicts being resolved
         * @property timeout How long before timeout
         * @property resolvedCount How many were resolved before timeout
         */
        public data class ResolutionTimeout(public val conflictCount: Int, public val timeout: Duration, public val resolvedCount: Int) :
            ConflictResolutionFailureType

        /**
         * Invalid resolution strategy.
         * @property strategy The invalid strategy name
         * @property availableStrategies Available valid strategies
         */
        public data class InvalidStrategy(public val strategy: String, public val availableStrategies: List<String>) : ConflictResolutionFailureType

        /**
         * Resolution resulted in data loss.
         * @property lostEventCount Number of events that would be lost
         * @property affectedAggregateIds Aggregates that would lose data
         */
        public data class DataLossDetected(public val lostEventCount: Int, public val affectedAggregateIds: List<String>) : ConflictResolutionFailureType
    }

    /**
     * Errors related to invalid input data.
     */
    public sealed interface InputError : DeviceSynchronizationContractError {
        /**
         * Invalid device ID format.
         * @property deviceId The invalid device ID
         * @property expectedFormat Expected format description
         */
        public data class InvalidDeviceId(public val deviceId: String, public val expectedFormat: String? = null) : InputError

        /**
         * Invalid sync window.
         * @property reason Specific reason for invalidity
         */
        public data class InvalidSyncWindow(public val reason: SyncWindowValidationFailure) : InputError

        /**
         * Invalid device status transition.
         * @property deviceId The device ID
         * @property currentStatus Current device status
         * @property requestedStatus Requested status
         */
        public data class InvalidStatusTransition(
            public val deviceId: String,
            public val currentStatus: DeviceStatus,
            public val requestedStatus: DeviceStatus,
        ) : InputError
    }

    /**
     * Sync window validation failures.
     */
    public sealed interface SyncWindowValidationFailure {
        public data class WindowTooLarge(public val requestedDays: Int, public val maximumDays: Int) : SyncWindowValidationFailure
        public data class InvalidDateRange(public val startDate: Instant, public val endDate: Instant) : SyncWindowValidationFailure
        public data object FutureDateNotAllowed : SyncWindowValidationFailure
    }

    /**
     * Errors related to business rule violations.
     */
    public sealed interface BusinessError : DeviceSynchronizationContractError {
        /**
         * Device not found.
         * @property deviceId The device ID that was not found
         * @property searchContext Optional context about where/how search was performed
         */
        public data class DeviceNotFound(public val deviceId: String, public val searchContext: String? = null) : BusinessError

        /**
         * Device registration failed.
         * @property failure Specific registration failure
         */
        public data class RegistrationFailed(public val failure: RegistrationFailureType) : BusinessError

        /**
         * Synchronization failed.
         * @property failure Specific synchronization failure
         */
        public data class SynchronizationFailed(public val failure: SynchronizationFailureType) : BusinessError

        /**
         * Conflict resolution failed.
         * @property failure Specific resolution failure
         */
        public data class ConflictResolutionFailed(public val failure: ConflictResolutionFailureType) : BusinessError

        /**
         * Device is in invalid state for operation.
         * @property deviceId The device ID
         * @property currentStatus Current device status
         * @property requiredStatuses Valid statuses for the operation
         */
        public data class InvalidDeviceState(
            public val deviceId: String,
            public val currentStatus: DeviceStatus,
            public val requiredStatuses: List<DeviceStatus>,
        ) : BusinessError

        /**
         * Sync already in progress.
         * @property localDeviceId Local device ID
         * @property remoteDeviceId Remote device ID
         * @property syncStartedAt When the current sync started
         * @property currentSyncStatus Current sync status
         */
        public data class SyncInProgress(
            public val localDeviceId: String,
            public val remoteDeviceId: String,
            public val syncStartedAt: Instant,
            public val currentSyncStatus: SyncStatus,
        ) : BusinessError

        /**
         * Device is offline.
         * @property deviceId The offline device ID
         * @property lastSeenAt When device was last online
         */
        public data class DeviceOffline(public val deviceId: String, public val lastSeenAt: Instant? = null) : BusinessError
    }

    /**
     * Errors related to system/infrastructure issues.
     */
    public sealed interface SystemError : DeviceSynchronizationContractError {
        /**
         * Service is temporarily unavailable.
         * @property service Service name
         * @property retryAfter Optional duration to wait before retry
         */
        public data class ServiceUnavailable(public val service: String, public val retryAfter: Duration? = null) : SystemError

        /**
         * Operation timeout.
         * @property operation Operation that timed out
         * @property timeout Timeout duration
         * @property hint Optional hint for resolution
         */
        public data class Timeout(public val operation: String, public val timeout: Duration, public val hint: String? = null) : SystemError

        /**
         * Storage quota exceeded.
         * @property deviceId Device that exceeded quota
         * @property usedBytes Bytes currently used
         * @property quotaBytes Quota limit in bytes
         */
        public data class StorageQuotaExceeded(public val deviceId: String, public val usedBytes: Long, public val quotaBytes: Long) : SystemError

        /**
         * Rate limit exceeded.
         * @property operation Rate-limited operation
         * @property limit Rate limit
         * @property resetAt When rate limit resets
         */
        public data class RateLimitExceeded(public val operation: String, public val limit: Int, public val resetAt: Instant) : SystemError
    }
}
