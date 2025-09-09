package io.github.kamiazya.scopes.collaborativeversioning.domain.entity

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Represents a review comment on a change proposal.
 *
 * Review comments allow reviewers (both human and AI agents) to provide feedback,
 * ask questions, or suggest modifications to a proposed change.
 */
data class ReviewComment(
    val id: String,
    val author: Author,
    val content: String,
    val commentType: ReviewCommentType,
    val proposedChangeId: String? = null, // Reference to specific proposed change if commenting on it
    val createdAt: Instant,
    val updatedAt: Instant,
    val resolved: Boolean = false,
    val resolvedBy: Author? = null,
    val resolvedAt: Instant? = null,
    val parentCommentId: String? = null, // For threaded discussions
) {
    /**
     * Mark this comment as resolved.
     */
    fun resolve(resolver: Author, timestamp: Instant = Clock.System.now()): ReviewComment = copy(
        resolved = true,
        resolvedBy = resolver,
        resolvedAt = timestamp,
        updatedAt = timestamp,
    )

    /**
     * Update the comment content.
     */
    fun updateContent(newContent: String, timestamp: Instant = Clock.System.now()): ReviewComment = copy(
        content = newContent,
        updatedAt = timestamp,
    )

    /**
     * Check if this comment is a reply to another comment.
     */
    fun isReply(): Boolean = parentCommentId != null

    /**
     * Check if this comment is about a specific proposed change.
     */
    fun isAboutProposedChange(): Boolean = proposedChangeId != null

    companion object {
        /**
         * Generate a unique ID for a review comment.
         */
        fun generateId(): String = "review_comment_${System.nanoTime()}"

        /**
         * Create a new review comment.
         */
        fun create(
            author: Author,
            content: String,
            commentType: ReviewCommentType,
            proposedChangeId: String? = null,
            parentCommentId: String? = null,
            timestamp: Instant = Clock.System.now(),
        ): ReviewComment = ReviewComment(
            id = generateId(),
            author = author,
            content = content,
            commentType = commentType,
            proposedChangeId = proposedChangeId,
            createdAt = timestamp,
            updatedAt = timestamp,
            parentCommentId = parentCommentId,
        )
    }
}

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
