package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Types of reviews that can be performed on a proposal.
 */
enum class ReviewType {
    /**
     * General comment without explicit approval/rejection.
     */
    COMMENT,

    /**
     * Approve the proposal.
     */
    APPROVE,

    /**
     * Reject the proposal with requested changes.
     */
    REJECT,

    /**
     * Request additional information or changes.
     */
    REQUEST_CHANGES,
}
