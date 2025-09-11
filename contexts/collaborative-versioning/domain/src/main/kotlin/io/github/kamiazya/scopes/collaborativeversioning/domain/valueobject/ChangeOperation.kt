package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Types of operations that can be performed on a resource.
 */
enum class ChangeOperation {
    ADD,
    MODIFY,
    DELETE,
    RENAME,
}
