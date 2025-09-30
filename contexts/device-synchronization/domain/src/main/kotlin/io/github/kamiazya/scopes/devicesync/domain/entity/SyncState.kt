package io.github.kamiazya.scopes.devicesync.domain.entity

import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import kotlinx.datetime.Instant
import org.jmolecules.ddd.types.AggregateRoot
import kotlin.time.Duration

/**
 * Represents the synchronization state between this device and another device.
 *
 * This entity encapsulates the business logic for managing synchronization state,
 * including state transitions, sync readiness checks, and error handling.
 *
 * Each SyncState is uniquely identified by the remote DeviceId it syncs with.
 */
data class SyncState(
    private val _deviceId: DeviceId,
    val lastSyncAt: Instant?,
    val remoteVectorClock: VectorClock,
    val lastSuccessfulPush: Instant?,
    val lastSuccessfulPull: Instant?,
    val syncStatus: SyncStatus,
    val pendingChanges: Int = 0,
) : AggregateRoot<SyncState, DeviceId> {

    /**
     */
    override fun getId(): DeviceId = _deviceId

    /**
     * Public accessor for deviceId.
     */
    val deviceId: DeviceId
        get() = _deviceId
    init {
        require(pendingChanges >= 0) { "Pending changes cannot be negative" }
        lastSuccessfulPush?.let { push ->
            lastSyncAt?.let { sync ->
                // Allow small clock precision differences (1 second tolerance)
                require(push.epochSeconds <= sync.epochSeconds + 1) { "Last successful push cannot be significantly after last sync" }
            }
        }
        lastSuccessfulPull?.let { pull ->
            lastSyncAt?.let { sync ->
                // Allow small clock precision differences (1 second tolerance)
                require(pull.epochSeconds <= sync.epochSeconds + 1) { "Last successful pull cannot be significantly after last sync" }
            }
        }
    }

    companion object {
        // Maximum allowed pending changes to prevent integer overflow
        const val MAX_PENDING_CHANGES = 1_000_000
    }

    /**
     * Check if synchronization is needed based on pending changes and status.
     */
    fun needsSync(): Boolean = when (syncStatus) {
        SyncStatus.IN_PROGRESS -> false // Already syncing
        SyncStatus.OFFLINE -> false // Device is offline
        else -> pendingChanges > 0 || syncStatus == SyncStatus.FAILED
    }

    /**
     * Check if the device is ready to sync.
     */
    fun canSync(): Boolean = syncStatus != SyncStatus.IN_PROGRESS && syncStatus != SyncStatus.OFFLINE

    /**
     * Start synchronization process.
     */
    fun startSync(now: Instant): SyncState {
        require(canSync()) { "Cannot start sync when status is $syncStatus" }
        return copy(
            syncStatus = SyncStatus.IN_PROGRESS,
            lastSyncAt = now,
        )
    }

    /**
     * Mark synchronization as successful.
     */
    fun markSyncSuccess(eventsPushed: Int, eventsPulled: Int, newRemoteVectorClock: VectorClock, now: Instant): SyncState {
        require(syncStatus == SyncStatus.IN_PROGRESS) { "Can only mark success when sync is in progress" }
        return copy(
            syncStatus = SyncStatus.SUCCESS,
            lastSuccessfulPush = if (eventsPushed > 0) now else lastSuccessfulPush,
            lastSuccessfulPull = if (eventsPulled > 0) now else lastSuccessfulPull,
            remoteVectorClock = newRemoteVectorClock,
            pendingChanges = 0,
        )
    }

    /**
     * Mark synchronization as failed.
     */
    fun markSyncFailed(reason: String? = null): SyncState {
        require(syncStatus == SyncStatus.IN_PROGRESS) { "Can only mark failure when sync is in progress" }
        return copy(
            syncStatus = SyncStatus.FAILED,
        )
    }

    /**
     * Mark device as offline.
     */
    fun markOffline(): SyncState = copy(syncStatus = SyncStatus.OFFLINE)

    /**
     * Mark device as online (back from offline).
     */
    fun markOnline(): SyncState = when (syncStatus) {
        SyncStatus.OFFLINE -> copy(
            syncStatus = if (lastSyncAt == null) SyncStatus.NEVER_SYNCED else SyncStatus.SUCCESS,
        )
        else -> this
    }

    /**
     * Increment pending changes counter.
     */
    fun incrementPendingChanges(count: Int = 1): SyncState {
        require(count > 0) { "Count must be positive" }
        val newPendingChanges = pendingChanges + count
        require(newPendingChanges <= MAX_PENDING_CHANGES) {
            "Pending changes would exceed maximum allowed value of $MAX_PENDING_CHANGES"
        }
        return copy(pendingChanges = newPendingChanges)
    }

    /**
     * Calculate time since last successful sync.
     */
    fun timeSinceLastSync(now: Instant): Duration? = lastSyncAt?.let { now - it }

    /**
     * Check if this sync state is stale (hasn't synced in a while).
     */
    fun isStale(threshold: Duration, now: Instant): Boolean = timeSinceLastSync(now)?.let { it > threshold } ?: true
}

/**
 * The current status of synchronization with a device.
 */
enum class SyncStatus {
    /** Never synchronized with this device */
    NEVER_SYNCED,

    /** Currently synchronizing */
    IN_PROGRESS,

    /** Last sync was successful */
    SUCCESS,

    /** Last sync failed */
    FAILED,

    /** Device is offline or unreachable */
    OFFLINE,
    ;

    /**
     * Checks if synchronization is currently active.
     */
    fun isActive(): Boolean = this == IN_PROGRESS

    /**
     * Checks if the status indicates a successful state.
     */
    fun isSuccessful(): Boolean = this == SUCCESS

    /**
     * Checks if the status indicates an error state.
     */
    fun isError(): Boolean = this == FAILED

    /**
     * Checks if the device is available for synchronization.
     */
    fun isAvailable(): Boolean = this != OFFLINE && this != IN_PROGRESS

    /**
     * Determines if a sync retry should be attempted.
     */
    fun shouldRetrySync(): Boolean = when (this) {
        NEVER_SYNCED, FAILED -> true
        SUCCESS, IN_PROGRESS, OFFLINE -> false
    }

    /**
     * Gets the next expected status after a sync attempt.
     */
    fun nextStatusOnSuccess(): SyncStatus = SUCCESS

    /**
     * Gets the next expected status after a sync failure.
     */
    fun nextStatusOnFailure(): SyncStatus = FAILED

    /**
     * Gets the progress state for UI representation.
     */
    fun getProgressState(): ProgressState = when (this) {
        NEVER_SYNCED -> ProgressState.NOT_STARTED
        IN_PROGRESS -> ProgressState.RUNNING
        SUCCESS -> ProgressState.COMPLETED
        FAILED -> ProgressState.ERROR
        OFFLINE -> ProgressState.BLOCKED
    }

    enum class ProgressState {
        NOT_STARTED,
        RUNNING,
        COMPLETED,
        ERROR,
        BLOCKED,
        ;

        fun isTerminal(): Boolean = this == COMPLETED || this == ERROR

        fun isActive(): Boolean = this == RUNNING

        fun canTransitionTo(target: ProgressState): Boolean = when (this) {
            NOT_STARTED -> target != COMPLETED // Can't go directly to completed
            RUNNING -> true // Can transition to any state
            COMPLETED, ERROR -> false // Terminal states
            BLOCKED -> target != COMPLETED // Can't complete from blocked
        }

        fun getCompletionPercentage(): Int = when (this) {
            NOT_STARTED -> 0
            RUNNING -> 50 // Assumed halfway
            COMPLETED -> 100
            ERROR -> 0
            BLOCKED -> 0
        }
    }

    /**
     * Gets the severity level for UI display (e.g., for color coding).
     */
    fun getSeverityLevel(): SeverityLevel = when (this) {
        SUCCESS -> SeverityLevel.INFO
        NEVER_SYNCED -> SeverityLevel.WARNING
        IN_PROGRESS -> SeverityLevel.INFO
        FAILED -> SeverityLevel.ERROR
        OFFLINE -> SeverityLevel.WARNING
    }

    enum class SeverityLevel {
        INFO,
        WARNING,
        ERROR,
        ;

        fun isError(): Boolean = this == ERROR

        fun requiresUserAttention(): Boolean = this != INFO

        fun getAlertPriority(): Int = when (this) {
            ERROR -> 3
            WARNING -> 2
            INFO -> 1
        }

        fun shouldBlockOperation(): Boolean = this == ERROR
    }
}
