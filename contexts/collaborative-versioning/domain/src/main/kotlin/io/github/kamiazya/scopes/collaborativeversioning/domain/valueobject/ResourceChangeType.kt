package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Types of changes that can be made to a tracked resource.
 */
enum class ResourceChangeType {
    /**
     * Initial creation of the resource.
     */
    CREATE,

    /**
     * Regular update to the resource content.
     */
    UPDATE,

    /**
     * Merge of multiple versions.
     */
    MERGE,

    /**
     * Restoration to a previous version.
     */
    RESTORE,

    /**
     * Import from external system.
     */
    IMPORT,
}
