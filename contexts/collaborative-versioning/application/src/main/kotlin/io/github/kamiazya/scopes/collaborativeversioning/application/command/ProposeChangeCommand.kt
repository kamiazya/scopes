package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ProposedChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId

/**
 * Command to create a new change proposal.
 *
 * This command initiates the proposal workflow by creating a new change proposal
 * in DRAFT state with the specified changes. The proposal can then be edited
 * before being submitted for review.
 */
data class ProposeChangeCommand(
    val author: Author,
    val targetResourceId: ResourceId,
    val title: String,
    val description: String,
    val proposedChanges: List<ProposedChange> = emptyList(),
)
