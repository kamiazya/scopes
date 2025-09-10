package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Event emitted when a proposal's changes are successfully merged/applied.
 *
 * This event marks the transition from APPROVED to APPLIED state,
 * indicating that the proposed changes have been successfully integrated
 * into the target resource.
 */
data class ChangeMerged(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    override val occurredAt: Instant,
    override val metadata: EventMetadata? = null,

    /**
     * The unique identifier of the merged proposal.
     */
    val proposalId: ProposalId,

    /**
     * The scope ID that was modified.
     */
    val targetScopeId: String,

    /**
     * The type of change that was applied.
     */
    val changeType: String,

    /**
     * Summary of what was changed.
     */
    val changeSummary: String,

    /**
     * The actual changes that were applied.
     * Structure depends on the changeType.
     */
    val appliedChanges: Map<String, Any>,

    /**
     * Version of the target resource before the merge.
     */
    val previousVersion: Long,

    /**
     * Version of the target resource after the merge.
     */
    val newVersion: Long,

    /**
     * ID of the user who performed the merge.
     */
    val mergedBy: String,

    /**
     * Strategy used for the merge (e.g., "fast-forward", "three-way").
     */
    val mergeStrategy: String,

    /**
     * Any conflicts that were resolved during the merge.
     */
    val resolvedConflicts: List<ConflictResolution> = emptyList(),
) : DomainEvent {

    companion object {
        /**
         * The stable type identifier for this event.
         */
        const val TYPE_ID = "collaborative-versioning.change.merged.v1"
    }
}

/**
 * Represents how a conflict was resolved during merge.
 */
data class ConflictResolution(
    /**
     * The field or property that had a conflict.
     */
    val field: String,

    /**
     * The conflicting value from the proposal.
     */
    val proposedValue: Any,

    /**
     * The conflicting value from the current state.
     */
    val currentValue: Any,

    /**
     * The resolved value that was applied.
     */
    val resolvedValue: Any,

    /**
     * Strategy used to resolve this conflict.
     */
    val resolutionStrategy: String,
)
