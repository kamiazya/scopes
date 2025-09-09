package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * States of a change proposal through its lifecycle.
 *
 * The state transitions are:
 * DRAFT → SUBMITTED → REVIEWING → APPROVED → APPLIED
 *                              ↘ REJECTED
 */
enum class ProposalState {
    /**
     * Initial state where the proposal is being created and can be edited.
     */
    DRAFT,

    /**
     * The proposal has been submitted for review and cannot be edited.
     */
    SUBMITTED,

    /**
     * The proposal is actively being reviewed.
     */
    REVIEWING,

    /**
     * The proposal has been approved and is ready to be applied.
     */
    APPROVED,

    /**
     * The proposal has been rejected and will not be applied.
     */
    REJECTED,

    /**
     * The proposal has been successfully applied to the target resource.
     */
    APPLIED,

    ;

    /**
     * Check if a transition to the target state is valid from this state.
     */
    fun canTransitionTo(targetState: ProposalState): Boolean = when (this) {
        DRAFT -> targetState == SUBMITTED
        SUBMITTED -> targetState == REVIEWING
        REVIEWING -> targetState == APPROVED || targetState == REJECTED
        APPROVED -> targetState == APPLIED
        REJECTED -> false // Terminal state
        APPLIED -> false // Terminal state
    }

    /**
     * Check if the proposal is in a terminal state.
     */
    fun isTerminal(): Boolean = this == REJECTED || this == APPLIED

    /**
     * Check if the proposal can be edited in this state.
     */
    fun isEditable(): Boolean = this == DRAFT

    /**
     * Check if the proposal can receive review comments in this state.
     */
    fun canReceiveComments(): Boolean = this == REVIEWING || this == APPROVED
}
