package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId

/**
 * Command to resolve a review comment.
 *
 * This marks a specific review comment as resolved.
 */
data class ResolveCommentCommand(val proposalId: ProposalId, val commentId: String, val resolver: Author)
