package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Represents the type of change made to a resource.
 */
enum class ChangeType {
    /**
     * A new resource was created
     */
    CREATE,

    /**
     * An existing resource was updated
     */
    UPDATE,

    /**
     * A resource was deleted
     */
    DELETE,

    /**
     * A resource was moved or renamed
     */
    MOVE,

    /**
     * Multiple resources were merged
     */
    MERGE,
}
