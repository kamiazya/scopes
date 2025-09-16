package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Types of resources that can be tracked for versioning.
 *
 * This enum represents the different kinds of resources that the versioning
 * system can manage. Each type may have specific behaviors or validation rules.
 */
enum class ResourceType {
    /**
     * A Scope resource - represents a hierarchical container in the system.
     * Scopes can contain other scopes, forming a tree structure for project organization.
     */
    SCOPE,

    /**
     * An Aspect resource - represents a cross-cutting concern or feature that can be
     * applied to multiple scopes. Examples include tags, deadlines, or custom attributes.
     */
    ASPECT,

    /**
     * An Alias resource - represents an alternative name or reference to another resource.
     * Aliases provide a way to create shortcuts or alternative naming schemes.
     */
    ALIAS,
}
