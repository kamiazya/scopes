package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Represents a conflict between a proposed change and the current resource state.
 */
data class ProposalConflict(val proposedChangeId: String, val path: String, val expectedValue: String?, val actualValue: String?, val description: String)
