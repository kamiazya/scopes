package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposedChange
import kotlinx.datetime.Instant

/**
 * Result of applying a change proposal.
 */
data class AppliedChanges(val proposalId: ProposalId, val proposedChanges: List<ProposedChange>, val appliedAt: Instant)
