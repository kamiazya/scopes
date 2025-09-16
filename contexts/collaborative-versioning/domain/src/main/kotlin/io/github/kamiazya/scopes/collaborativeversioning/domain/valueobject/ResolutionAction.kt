package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Actions for resolving conflicts.
 */
enum class ResolutionAction {
    TakeFirst, // Use change from first change set
    TakeSecond, // Use change from second change set
    Skip, // Skip both changes
    Fail, // Fail and require manual resolution
}
