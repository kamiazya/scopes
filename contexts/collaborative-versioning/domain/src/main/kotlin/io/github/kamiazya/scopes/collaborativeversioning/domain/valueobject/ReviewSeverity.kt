package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Severity levels for review feedback.
 */
enum class ReviewSeverity {
    /**
     * Information only, no action required.
     */
    INFO,

    /**
     * Minor issue, should be considered but not blocking.
     */
    WARNING,

    /**
     * Major issue, must be addressed before approval.
     */
    ERROR,

    /**
     * Critical issue, proposal cannot proceed without resolution.
     */
    BLOCKER,
}
