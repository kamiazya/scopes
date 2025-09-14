package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Represents the status/state of a scope in its lifecycle.
 *
 * This sealed class enforces valid state transitions at the domain level,
 * ensuring that scopes can only move between states in meaningful ways.
 *
 * ## State Transitions
 * ```
 * Draft ──→ Active ──→ Completed
 *   ↓         ↓ ↑          ↓
 *   └────→ Archived ←──────┘
 * ```
 *
 * @since 1.0.0
 * @see Scope
 */
sealed class ScopeStatus {

    /**
     * Draft status - scope is being created but not yet active.
     *
     * In this state:
     * - The scope can be edited freely
     * - Children can be added
     * - Can transition to: Active, Archived
     */
    object Draft : ScopeStatus()

    /**
     * Active status - scope is actively being worked on.
     *
     * In this state:
     * - The scope can be edited
     * - Children can be added
     * - Work is in progress
     * - Can transition to: Completed, Archived
     */
    object Active : ScopeStatus()

    /**
     * Completed status - scope has been completed.
     *
     * In this state:
     * - The scope is read-only (no edits)
     * - No new children can be added
     * - Work has been finished
     * - Can transition to: Active (reopened), Archived
     */
    object Completed : ScopeStatus()

    /**
     * Archived status - scope is no longer active but kept for reference.
     *
     * In this state:
     * - The scope is read-only
     * - No new children can be added
     * - Kept for historical purposes
     * - Can transition to: Active (reactivated)
     */
    object Archived : ScopeStatus()

    /**
     * Validates if a transition from current status to target status is allowed.
     */
    fun canTransitionTo(target: ScopeStatus): Boolean = when (this) {
        is Draft -> target is Active || target is Archived
        is Active -> target is Completed || target is Archived
        is Completed -> target is Active || target is Archived
        is Archived -> target is Active // Can be reactivated
    }

    /**
     * Attempts to transition to a new status.
     */
    fun transitionTo(target: ScopeStatus): Either<ScopesError.ScopeStatusTransitionError, ScopeStatus> = if (canTransitionTo(target)) {
        target.right()
    } else {
        ScopesError.ScopeStatusTransitionError(
            from = this.toString(),
            to = target.toString(),
            reason = "Invalid state transition from $this to $target",
        ).left()
    }

    /**
     * Checks if the scope is in a terminal state.
     */
    fun isTerminal(): Boolean = this is Archived

    /**
     * Checks if the scope can have children added.
     */
    fun canAddChildren(): Boolean = this is Draft || this is Active

    /**
     * Checks if the scope can be edited.
     */
    fun canBeEdited(): Boolean = this is Draft || this is Active

    companion object {
        /**
         * Creates a scope status from string representation.
         */
        fun fromString(value: String): Either<IllegalArgumentException, ScopeStatus> = when (value.uppercase()) {
            "DRAFT" -> Draft.right()
            "ACTIVE" -> Active.right()
            "COMPLETED" -> Completed.right()
            "ARCHIVED" -> Archived.right()
            else -> IllegalArgumentException("Invalid scope status: $value").left()
        }

        /**
         * Default status for new scopes.
         */
        fun default(): ScopeStatus = Draft
    }

    override fun toString(): String = when (this) {
        is Draft -> "DRAFT"
        is Active -> "ACTIVE"
        is Completed -> "COMPLETED"
        is Archived -> "ARCHIVED"
    }
}
