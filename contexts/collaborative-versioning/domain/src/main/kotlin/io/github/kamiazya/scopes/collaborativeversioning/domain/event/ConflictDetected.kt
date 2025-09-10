package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Event emitted when a conflict is detected during merge attempt.
 *
 * This event captures situations where the proposed changes cannot be
 * automatically merged due to conflicts with the current state of the
 * target resource.
 */
data class ConflictDetected(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    override val occurredAt: Instant,
    override val metadata: EventMetadata? = null,

    /**
     * The unique identifier of the proposal with conflicts.
     */
    val proposalId: ProposalId,

    /**
     * The scope ID that the proposal targets.
     */
    val targetScopeId: String,

    /**
     * Details of the detected conflicts.
     */
    val conflicts: List<Conflict>,

    /**
     * Version of the target when the proposal was created.
     */
    val proposalBaseVersion: Long,

    /**
     * Current version of the target resource.
     */
    val currentTargetVersion: Long,

    /**
     * ID of the user who triggered the merge attempt.
     */
    val detectedBy: String,

    /**
     * Suggested resolution strategies for the conflicts.
     */
    val suggestedResolutions: List<ResolutionSuggestion> = emptyList(),
) : DomainEvent {

    companion object {
        /**
         * The stable type identifier for this event.
         */
        const val TYPE_ID = "collaborative-versioning.conflict.detected.v1"
    }
}

/**
 * Represents a single conflict between proposed and current state.
 */
data class Conflict(
    /**
     * Type of conflict (e.g., "concurrent_update", "deleted_field").
     */
    val type: ConflictType,

    /**
     * The field or property with the conflict.
     */
    val field: String,

    /**
     * Value from the proposal.
     */
    val proposedValue: Any?,

    /**
     * Current value in the target.
     */
    val currentValue: Any?,

    /**
     * Original value when the proposal was created.
     */
    val baseValue: Any?,

    /**
     * Human-readable description of the conflict.
     */
    val description: String,
)

/**
 * Types of conflicts that can occur.
 */
enum class ConflictType {
    /**
     * Both proposal and target modified the same field.
     */
    CONCURRENT_UPDATE,

    /**
     * Proposal modifies a field that was deleted.
     */
    DELETED_FIELD,

    /**
     * Proposal deletes a field that was modified.
     */
    MODIFIED_DELETED,

    /**
     * Structural conflict that prevents merge.
     */
    STRUCTURAL,

    /**
     * Semantic conflict based on business rules.
     */
    SEMANTIC,
}

/**
 * Suggestion for resolving a conflict.
 */
data class ResolutionSuggestion(
    /**
     * The conflict this suggestion addresses.
     */
    val conflictField: String,

    /**
     * Suggested resolution strategy.
     */
    val strategy: ResolutionStrategy,

    /**
     * Explanation of the suggestion.
     */
    val rationale: String,
)

/**
 * Strategies for resolving conflicts.
 */
enum class ResolutionStrategy {
    /**
     * Use the value from the proposal.
     */
    USE_PROPOSED,

    /**
     * Keep the current value.
     */
    KEEP_CURRENT,

    /**
     * Merge both values (if applicable).
     */
    MERGE_VALUES,

    /**
     * Requires manual resolution.
     */
    MANUAL,

    /**
     * Rebase the proposal on current state.
     */
    REBASE,
}
