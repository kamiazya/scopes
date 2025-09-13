package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Types of review comments.
 */
enum class ReviewCommentType {
    /**
     * General comment or feedback.
     */
    COMMENT,

    /**
     * Suggestion for improvement.
     */
    SUGGESTION,

    /**
     * Issue that must be addressed before approval.
     */
    ISSUE,

    /**
     * Question requiring clarification.
     */
    QUESTION,

    /**
     * Approval of the proposal or specific change.
     */
    APPROVAL,

    /**
     * Request for changes.
     */
    REQUEST_CHANGES,
}
