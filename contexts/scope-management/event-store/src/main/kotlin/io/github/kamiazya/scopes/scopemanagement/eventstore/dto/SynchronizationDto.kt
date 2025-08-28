package io.github.kamiazya.scopes.scopemanagement.eventstore.dto

import io.github.kamiazya.scopes.scopemanagement.eventstore.model.StoredEvent
import io.github.kamiazya.scopes.scopemanagement.eventstore.model.VectorClock

/**
 * Result of a push operation.
 */
data class PushResult(val eventsPushed: Int, val remoteVectorClock: VectorClock)

/**
 * Result of a pull operation.
 */
data class PullResult(val eventsPulled: Int, val conflicts: List<Pair<StoredEvent, StoredEvent>>, val newLocalVectorClock: VectorClock)

/**
 * Result of a full synchronization.
 */
data class SynchronizationResult(val pushResult: PushResult, val pullResult: PullResult, val conflictsResolved: Int)

/**
 * Result of conflict resolution.
 */
data class ConflictResolutionResult(val resolvedConflicts: Int, val unresolvedConflicts: Int, val actions: List<ConflictResolutionAction>)

/**
 * Actions taken during conflict resolution.
 */
sealed class ConflictResolutionAction {
    data class KeepLocal(val localEvent: StoredEvent) : ConflictResolutionAction()
    data class KeepRemote(val remoteEvent: StoredEvent) : ConflictResolutionAction()
    data class Merge(val localEvent: StoredEvent, val remoteEvent: StoredEvent, val mergedEvent: StoredEvent) : ConflictResolutionAction()
    data class ManualReview(val localEvent: StoredEvent, val remoteEvent: StoredEvent, val reason: String) : ConflictResolutionAction()
}

/**
 * Strategies for resolving conflicts between events.
 */
sealed class ConflictResolutionStrategy {
    /**
     * Always keep the local version of conflicting events.
     */
    object KeepLocal : ConflictResolutionStrategy()

    /**
     * Always keep the remote version of conflicting events.
     */
    object KeepRemote : ConflictResolutionStrategy()

    /**
     * Keep the version with the latest timestamp.
     */
    object LatestTimestamp : ConflictResolutionStrategy()

    /**
     * Use a custom resolution function.
     */
    data class Custom(val resolver: suspend (StoredEvent, StoredEvent) -> ConflictResolutionAction) : ConflictResolutionStrategy()

    /**
     * Mark conflicts for manual review.
     */
    object ManualReview : ConflictResolutionStrategy()
}
