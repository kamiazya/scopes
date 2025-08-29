package io.github.kamiazya.scopes.devicesync.application.error

import io.github.kamiazya.scopes.platform.application.error.ApplicationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Application-level errors for device synchronization operations.
 */
sealed class DeviceSyncApplicationError : ApplicationError {

    /**
     * Repository operation failed.
     */
    data class RepositoryError(
        val operation: RepositoryOperation,
        val entityType: String,
        val entityId: String? = null,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : DeviceSyncApplicationError()

    enum class RepositoryOperation {
        SAVE,
        FIND,
        DELETE,
        UPDATE,
        QUERY,
    }

    /**
     * Synchronization operation failed.
     */
    data class SyncOperationError(
        val operation: SyncOperation,
        val deviceId: String,
        val remoteDeviceId: String? = null,
        val failureReason: SyncFailureReason? = null,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : DeviceSyncApplicationError()

    enum class SyncOperation {
        PULL,
        PUSH,
        FULL_SYNC,
        CONFLICT_RESOLUTION,
        HANDSHAKE,
    }

    enum class SyncFailureReason {
        NETWORK_ERROR,
        AUTHENTICATION_FAILED,
        VERSION_MISMATCH,
        TIMEOUT,
        DATA_CORRUPTION,
    }

    /**
     * Event store operation failed.
     */
    data class EventStoreError(
        val operation: EventStoreOperation,
        val aggregateId: String,
        val eventType: String? = null,
        val eventCount: Int? = null,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : DeviceSyncApplicationError()

    enum class EventStoreOperation {
        APPEND,
        LOAD,
        LOAD_STREAM,
        SNAPSHOT,
        QUERY_EVENTS,
    }

    /**
     * Input validation failed.
     */
    data class ValidationError(
        val fieldName: String,
        val invalidValue: Any?,
        val validationRule: ValidationRule,
        val context: String? = null,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : DeviceSyncApplicationError()

    enum class ValidationRule {
        REQUIRED,
        FORMAT_INVALID,
        OUT_OF_RANGE,
        TOO_LONG,
        TOO_SHORT,
        DUPLICATE,
        INVALID_STATE,
    }
}
