package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Rule for custom merge strategy.
 */
data class ResolutionRule(val conflictType: ConflictType, val resolution: ResolutionAction, val condition: String? = null)
