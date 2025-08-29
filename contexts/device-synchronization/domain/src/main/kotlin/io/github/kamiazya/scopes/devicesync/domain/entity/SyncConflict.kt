package io.github.kamiazya.scopes.devicesync.domain.entity

import io.github.kamiazya.scopes.devicesync.domain.service.ConflictType
import io.github.kamiazya.scopes.devicesync.domain.service.ResolutionAction
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Represents a synchronization conflict with rich domain logic for resolution.
 *
 * This entity encapsulates the business rules for conflict detection, analysis,
 * and resolution strategies.
 */
data class SyncConflict(
    val localEventId: String,
    val remoteEventId: String,
    val aggregateId: String,
    val localVersion: Long,
    val remoteVersion: Long,
    val localVectorClock: VectorClock,
    val remoteVectorClock: VectorClock,
    val conflictType: ConflictType,
    val detectedAt: Instant = Clock.System.now(),
    val resolvedAt: Instant? = null,
    val resolution: ResolutionAction? = null,
) {
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
        localVectorClock.happenedBefore(remoteVectorClock) -> ResolutionAction.KEPT_REMOTE

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
    fun resolve(action: ResolutionAction): SyncConflict {
        require(isPending()) { "Cannot resolve an already resolved conflict" }
        return copy(
            resolution = action,
            resolvedAt = Clock.System.now(),
        )
    }

    /**
     * Mark conflict as deferred (needs manual resolution).
     */
    fun defer(): SyncConflict = resolve(ResolutionAction.DEFERRED)

    /**
     * Create a merge resolution if both versions can be combined.
     */
    fun merge(): SyncConflict = resolve(ResolutionAction.MERGED)

    companion object {
        /**
         * Detect conflicts between local and remote events.
         */
        fun detect(
            localEventId: String,
            remoteEventId: String,
            aggregateId: String,
            localVersion: Long,
            remoteVersion: Long,
            localVectorClock: VectorClock,
            remoteVectorClock: VectorClock,
        ): SyncConflict? {
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

            return SyncConflict(
                localEventId = localEventId,
                remoteEventId = remoteEventId,
                aggregateId = aggregateId,
                localVersion = localVersion,
                remoteVersion = remoteVersion,
                localVectorClock = localVectorClock,
                remoteVectorClock = remoteVectorClock,
                conflictType = conflictType,
            )
        }
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
