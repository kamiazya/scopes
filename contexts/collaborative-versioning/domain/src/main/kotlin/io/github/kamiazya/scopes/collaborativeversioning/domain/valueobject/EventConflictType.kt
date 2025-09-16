package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Types of conflicts that can occur in events.
 */
enum class EventConflictType {
    /**
     * Both proposal and target modified the same field.
     */
    CONCURRENT_UPDATE,

    /**
     * Proposal modifies a field that was deleted.
     */
    DELETED_FIELD,

    /**
     * Proposal deletes a field that was modified.
     */
    MODIFIED_DELETED,

    /**
     * Structural conflict that prevents merge.
     */
    STRUCTURAL,

    /**
     * Semantic conflict based on business rules.
     */
    SEMANTIC,
}
