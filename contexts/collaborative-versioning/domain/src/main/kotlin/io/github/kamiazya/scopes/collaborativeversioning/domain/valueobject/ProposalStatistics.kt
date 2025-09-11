package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Statistics about a change proposal.
 */
data class ProposalStatistics(
    val proposedChangeCount: Int,
    val totalCommentCount: Int,
    val unresolvedCommentCount: Int,
    val issueCount: Int,
    val unresolvedIssueCount: Int,
)
