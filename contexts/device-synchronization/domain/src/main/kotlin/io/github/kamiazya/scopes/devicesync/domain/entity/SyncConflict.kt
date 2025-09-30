package io.github.kamiazya.scopes.devicesync.domain.entity

import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictType
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ResolutionAction
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import kotlinx.datetime.Instant
import org.jmolecules.ddd.types.Entity

/**
 * Represents a synchronization conflict with rich domain logic for resolution.
 *
 * This entity encapsulates the business rules for conflict detection, analysis,
 * and resolution strategies.
 *
 */
data class SyncConflict(
    private val _id: ConflictId,
    val localEventId: String,
    val remoteEventId: String,
    val aggregateId: String,
    val localVersion: Long,
    val remoteVersion: Long,
    val localVectorClock: VectorClock,
    val remoteVectorClock: VectorClock,
    val conflictType: ConflictType,
    val detectedAt: Instant,
    val resolvedAt: Instant? = null,
    val resolution: ResolutionAction? = null,
) : Entity<SyncState, ConflictId> {

    /**
     * Use getId() to access the conflict ID.
     */
    override fun getId(): ConflictId = _id

    init {
        require(localEventId.isNotBlank()) { "Local event ID cannot be blank" }
        require(remoteEventId.isNotBlank()) { "Remote event ID cannot be blank" }
        require(aggregateId.isNotBlank()) { "Aggregate ID cannot be blank" }
        require(localVersion >= 0) { "Local version must be non-negative" }
        require(remoteVersion >= 0) { "Remote version must be non-negative" }
        resolvedAt?.let {
            // Allow small clock precision differences (1 second tolerance)
            require(it.epochSeconds >= detectedAt.epochSeconds - 1) { "Resolved time cannot be significantly before detected time" }
            requireNotNull(resolution) { "Resolution must be provided when resolvedAt is set" }
        }
        resolution?.let {
            requireNotNull(resolvedAt) { "ResolvedAt must be provided when resolution is set" }
        }
    }

    /**
     * Check if this conflict has been resolved.
     */
    fun isResolved(): Boolean = resolution != null && resolvedAt != null

    /**
     * Check if this conflict is pending resolution.
     */
    fun isPending(): Boolean = !isResolved()

    /**
     * Determine if this is a true conflict or just a concurrent event.
     */
    fun isTrueConflict(): Boolean = when (conflictType) {
        ConflictType.CONCURRENT_MODIFICATION -> {
            // Check if vector clocks indicate true concurrency
            localVectorClock.isConcurrentWith(remoteVectorClock)
        }
        ConflictType.VERSION_MISMATCH -> {
            // Version mismatch is always a conflict
            true
        }
        ConflictType.MISSING_DEPENDENCY -> {
            // Missing dependencies are always conflicts
            true
        }
        ConflictType.DELETED_MODIFIED -> {
            // Deleted/modified conflicts are always true conflicts
            true
        }
        ConflictType.SCHEMA_MISMATCH -> {
            // Schema mismatches are always conflicts
            true
        }
    }

    /**
     * Determine the severity of the conflict.
     */
    fun severity(): ConflictSeverity = when {
        conflictType == ConflictType.MISSING_DEPENDENCY -> ConflictSeverity.CRITICAL
        conflictType == ConflictType.VERSION_MISMATCH && kotlin.math.abs(localVersion - remoteVersion) > 1 -> ConflictSeverity.HIGH
        conflictType == ConflictType.CONCURRENT_MODIFICATION -> ConflictSeverity.MEDIUM
        else -> ConflictSeverity.LOW
    }

    /**
     * Suggest a resolution strategy based on the conflict type and vector clocks.
     */
    fun suggestResolution(): ResolutionAction = when {
        // If local happened before remote, keep remote
        localVectorClock.happenedBefore(remoteVectorClock) -> ResolutionAction.ACCEPTED_REMOTE

        // If remote happened before local, keep local
        remoteVectorClock.happenedBefore(localVectorClock) -> ResolutionAction.KEPT_LOCAL

        // For true concurrent events, we need more sophisticated logic
        conflictType == ConflictType.CONCURRENT_MODIFICATION -> {
            // Could use timestamps, device priority, or other heuristics
            // For now, default to manual review
            ResolutionAction.DEFERRED
        }

        // Missing dependencies always need manual review
        conflictType == ConflictType.MISSING_DEPENDENCY -> ResolutionAction.DEFERRED

        else -> ResolutionAction.DEFERRED
    }

    /**
     * Resolve the conflict with a specific action.
     */
    fun resolve(action: ResolutionAction, now: Instant): SyncConflict {
        require(isPending()) { "Cannot resolve an already resolved conflict" }
        return copy(
            resolution = action,
            resolvedAt = now,
        )
    }

    /**
     * Mark conflict as deferred (needs manual resolution).
     */
    fun defer(now: Instant): SyncConflict = resolve(ResolutionAction.DEFERRED, now)

    /**
     * Create a merge resolution if both versions can be combined.
     */
    fun merge(now: Instant): SyncConflict = resolve(ResolutionAction.MERGED, now)

    /**
     * Parameters for detecting sync conflicts.
     */
    data class DetectionParams(
        val localEventId: String,
        val remoteEventId: String,
        val aggregateId: String,
        val localVersion: Long,
        val remoteVersion: Long,
        val localVectorClock: VectorClock,
        val remoteVectorClock: VectorClock,
        val detectedAt: Instant,
    )

    companion object {
        /**
         * Detect conflicts between local and remote events.
         */
        fun detect(params: DetectionParams): SyncConflict? {
            val (
                localEventId,
                remoteEventId,
                aggregateId,
                localVersion,
                remoteVersion,
                localVectorClock,
                remoteVectorClock,
                detectedAt,
            ) = params
            // No conflict if vector clocks show clear causality
            if (localVectorClock.happenedBefore(remoteVectorClock) ||
                remoteVectorClock.happenedBefore(localVectorClock)
            ) {
                return null
            }

            // Determine conflict type
            val conflictType = when {
                localVectorClock.isConcurrentWith(remoteVectorClock) -> ConflictType.CONCURRENT_MODIFICATION
                localVersion != remoteVersion -> ConflictType.VERSION_MISMATCH
                else -> return null // No conflict
            }

            require(localEventId.isNotBlank()) { "Local event ID cannot be blank" }
            require(remoteEventId.isNotBlank()) { "Remote event ID cannot be blank" }
            require(aggregateId.isNotBlank()) { "Aggregate ID cannot be blank" }
            require(localVersion >= 0) { "Local version must be non-negative" }
            require(remoteVersion >= 0) { "Remote version must be non-negative" }

            return SyncConflict(
                _id = ConflictId.generate(),
                localEventId = localEventId,
                remoteEventId = remoteEventId,
                aggregateId = aggregateId,
                localVersion = localVersion,
                remoteVersion = remoteVersion,
                localVectorClock = localVectorClock,
                remoteVectorClock = remoteVectorClock,
                conflictType = conflictType,
                detectedAt = detectedAt,
            )
        }

        /**
         * Create a new SyncConflict for testing purposes.
         * Auto-generates an ID.
         */
        fun create(
            localEventId: String,
            remoteEventId: String,
            aggregateId: String,
            localVersion: Long,
            remoteVersion: Long,
            localVectorClock: VectorClock,
            remoteVectorClock: VectorClock,
            conflictType: ConflictType,
            detectedAt: Instant,
            resolvedAt: Instant? = null,
            resolution: ResolutionAction? = null,
        ): SyncConflict = SyncConflict(
            _id = ConflictId.generate(),
            localEventId = localEventId,
            remoteEventId = remoteEventId,
            aggregateId = aggregateId,
            localVersion = localVersion,
            remoteVersion = remoteVersion,
            localVectorClock = localVectorClock,
            remoteVectorClock = remoteVectorClock,
            conflictType = conflictType,
            detectedAt = detectedAt,
            resolvedAt = resolvedAt,
            resolution = resolution,
        )
    }
}

/**
 * Severity levels for conflicts.
 */
enum class ConflictSeverity {
    LOW, // Minor conflicts that can be auto-resolved
    MEDIUM, // Conflicts that need attention but aren't blocking
    HIGH, // Serious conflicts that may cause data inconsistency
    CRITICAL, // Conflicts that must be resolved immediately
    ;

    /**
     * Checks if this conflict requires immediate attention.
     */
    fun requiresImmediateAttention(): Boolean = this == CRITICAL || this == HIGH

    /**
     * Checks if this conflict can be auto-resolved.
     */
    fun canAutoResolve(): Boolean = this == LOW

    /**
     * Gets the priority level for resolution (higher number = higher priority).
     */
    fun getPriority(): Int = when (this) {
        CRITICAL -> 4
        HIGH -> 3
        MEDIUM -> 2
        LOW -> 1
    }

    /**
     * Determines if this severity is more critical than another.
     */
    fun isMoreCriticalThan(other: ConflictSeverity): Boolean = this.getPriority() > other.getPriority()

    /**
     * Gets the notification level for this severity.
     */
    fun getNotificationLevel(): NotificationLevel = when (this) {
        CRITICAL -> NotificationLevel.ALERT
        HIGH -> NotificationLevel.WARNING
        MEDIUM -> NotificationLevel.INFO
        LOW -> NotificationLevel.DEBUG
    }

    enum class NotificationLevel {
        ALERT,
        WARNING,
        INFO,
        DEBUG,
        ;

        fun shouldNotifyUser(): Boolean = this == ALERT || this == WARNING

        fun shouldLogToSystem(): Boolean = true

        fun getPriority(): Int = when (this) {
            ALERT -> 4
            WARNING -> 3
            INFO -> 2
            DEBUG -> 1
        }
    }
}
