package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Type of line in diff.
 */
enum class LineType {
    Added,
    Removed,
    Modified,
    Unchanged,
    Context,
}
