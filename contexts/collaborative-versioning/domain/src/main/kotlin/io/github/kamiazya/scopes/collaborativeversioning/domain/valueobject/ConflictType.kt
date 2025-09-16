package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Types of conflicts that can be detected.
 */
enum class ConflictType {
    UpdateConflict, // Same field updated differently
    DeleteUpdateConflict, // One deletes, other updates
    StructuralConflict, // Hierarchical structure conflicts
    MoveConflict, // Conflicting move operations
    AddConflict, // Both add to same location
    DoubleDelete, // Both delete same field
    SemanticConflict, // Business rule violations
    Unknown, // Unclassified conflict
}
