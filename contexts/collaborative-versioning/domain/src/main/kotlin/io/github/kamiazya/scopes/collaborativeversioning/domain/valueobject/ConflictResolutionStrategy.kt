package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Strategies for resolving conflicts.
 */
enum class ConflictResolutionStrategy {
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
