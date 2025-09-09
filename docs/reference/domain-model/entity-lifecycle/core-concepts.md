# Entity Lifecycle - Core Concepts

This document describes the core concepts of the Entity Lifecycle system, which provides generic change management, versioning, and AI collaboration capabilities for all entity types in the Scopes system.

## Overview

The Entity Lifecycle system enables:
- **Universal Change Tracking**: Track changes for any entity type (Scope, UserPreferences, Device, etc.)
- **Version Management**: Create branches, experiment safely, and merge changes
- **AI Collaboration**: AI agents can propose changes with full attribution and user oversight
- **Complete Audit Trail**: Every change is recorded with who, what, when, and why

## Core Value Objects

### EntityType

Identifies the type of entity being managed.

```kotlin
data class EntityType(val value: String) {
    companion object {
        val SCOPE = EntityType("Scope")
        val USER_PREFERENCES = EntityType("UserPreferences")
        val DEVICE = EntityType("Device")
        // Additional types can be registered dynamically
    }
}
```

**Usage**:
- Acts as a discriminator for routing entity-specific operations
- Enables pluggable AI strategies per entity type
- Used for querying and filtering changes by entity type

### EntityId

Universal identifier for any entity in the system.

```kotlin
data class EntityId(val value: String) {
    init {
        require(value.isNotBlank()) { "EntityId cannot be blank" }
    }
}
```

**Key Properties**:
- **Universally Unique**: Can identify any entity across all contexts
- **Opaque**: Internal structure is hidden from the lifecycle system
- **Context-Convertible**: Can be converted to/from context-specific IDs (ScopeId, etc.)

### FieldPath

JSON-Path style identifier for entity fields and nested properties.

```kotlin
data class FieldPath(val segments: List<String>) {
    constructor(path: String) : this(path.split('.'))
    
    fun append(segment: String): FieldPath = FieldPath(segments + segment)
    fun parent(): FieldPath? = if (segments.size > 1) FieldPath(segments.dropLast(1)) else null
    
    override fun toString(): String = segments.joinToString(".")
}
```

**Examples**:
- `FieldPath("title")` - Simple field
- `FieldPath("preferences.hierarchy.showCompleted")` - Nested field
- `FieldPath("children[2].title")` - Array element field

**Usage**:
- Precise change tracking at field level
- Enables conflict detection during merges
- Supports complex entity structures

### VersionId

Identifies a specific version/branch of an entity.

```kotlin
data class VersionId(val value: String) {
    companion object {
        val MAIN = VersionId("main")
        fun generate(): VersionId = VersionId(ULID.generate())
    }
}
```

**Version Types**:
- **Main Version**: `VersionId.MAIN` - the primary entity state
- **Feature Branches**: User-created versions for experimentation
- **AI Experiment Branches**: AI-created versions for safe proposal testing

### CausedBy

Attribution for who/what caused a change.

```kotlin
sealed class CausedBy {
    data class User(val userId: UserId) : CausedBy()
    data class AIAgent(val agentId: AgentId) : CausedBy()
    object System : CausedBy()
}
```

**Purpose**:
- Complete attribution for all changes
- Enables AI-specific workflows and UI
- Supports audit and rollback scenarios

## Core Entities

### EntityChange

Represents a single atomic change to an entity field.

```kotlin
data class EntityChange(
    val id: ChangeId,
    val entityType: EntityType,
    val entityId: EntityId,
    val fieldPath: FieldPath,
    val operation: ChangeOperation,
    val beforeValue: JsonValue?,
    val afterValue: JsonValue?,
    val version: VersionId,
    val causedBy: CausedBy,
    val timestamp: Instant,
    val metadata: ChangeMetadata = ChangeMetadata()
)

enum class ChangeOperation {
    CREATE,  // Field/entity created
    UPDATE,  // Field value changed
    DELETE   // Field/entity removed
}
```

**Key Properties**:
- **Atomic**: Each change represents one field modification
- **Immutable**: Changes cannot be modified once created
- **Versioned**: Changes are associated with specific entity versions
- **Attributed**: Every change has clear attribution

### EntityVersion

Represents a specific version/branch of an entity.

```kotlin
data class EntityVersion(
    val id: VersionId,
    val entityType: EntityType,
    val entityId: EntityId,
    val name: VersionName,
    val parentVersion: VersionId?,
    val status: VersionStatus,
    val metadata: VersionMetadata,
    val createdAt: Instant
)

enum class VersionStatus {
    ACTIVE,     // Currently being worked on
    MERGED,     // Merged into another version
    ABANDONED   // No longer being worked on
}

data class VersionName(val value: String) {
    companion object {
        val MAIN = VersionName("main")
    }
}
```

**Version Hierarchy**:
- Versions form a tree structure through `parentVersion`
- Main version is the root of the tree
- Branches can be created from any version
- Multiple levels of branching are supported

### EntitySnapshot

Complete state of an entity at a specific version and point in time.

```kotlin
data class EntitySnapshot(
    val entityType: EntityType,
    val entityId: EntityId,
    val version: VersionId,
    val timestamp: Instant,
    val data: JsonObject,
    val checksum: ChecksumValue,
    val metadata: SnapshotMetadata = SnapshotMetadata()
)

data class ChecksumValue(val value: String) {
    companion object {
        fun calculate(data: JsonObject): ChecksumValue {
            // SHA-256 hash of normalized JSON
            val normalized = Json.encodeToString(JsonObject.serializer(), data)
            return ChecksumValue(MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray()).toHex())
        }
    }
}
```

**Purpose**:
- **Performance**: Fast entity reconstruction without replaying all changes
- **Integrity**: Checksum ensures data hasn't been corrupted
- **Time Travel**: View entity state at any point in history

## Domain Services

### EntityChangeService

Core service for recording and querying entity changes.

```kotlin
interface EntityChangeService {
    /**
     * Record a change to an entity field
     */
    suspend fun recordChange(change: EntityChange): Result<ChangeId, ChangeRecordingError>
    
    /**
     * Record multiple changes atomically (same timestamp, causedBy)
     */
    suspend fun recordChanges(changes: List<EntityChange>): Result<List<ChangeId>, ChangeRecordingError>
    
    /**
     * Get all changes for an entity, optionally since a specific time
     */
    suspend fun getChanges(
        entityId: EntityId, 
        since: Instant? = null,
        version: VersionId? = null
    ): Result<List<EntityChange>, QueryError>
    
    /**
     * Get changes for specific field path
     */
    suspend fun getFieldChanges(
        entityId: EntityId,
        fieldPath: FieldPath,
        version: VersionId? = null
    ): Result<List<EntityChange>, QueryError>
}
```

### EntityVersionService

Service for managing entity versions and branches.

```kotlin
interface EntityVersionService {
    /**
     * Create a new version/branch from existing version
     */
    suspend fun createVersion(request: CreateVersionRequest): Result<EntityVersion, VersionCreationError>
    
    /**
     * Switch entity to different version
     */
    suspend fun switchVersion(
        entityId: EntityId, 
        targetVersion: VersionId
    ): Result<EntitySnapshot, SwitchError>
    
    /**
     * Merge changes from source version into target version
     */
    suspend fun mergeVersion(
        sourceVersion: VersionId,
        targetVersion: VersionId,
        strategy: MergeStrategy = MergeStrategy.AUTOMATIC
    ): Result<MergeResult, MergeError>
    
    /**
     * Get all versions for an entity
     */
    suspend fun getVersions(entityId: EntityId): Result<List<EntityVersion>, QueryError>
}
```

## Change Detection Overview

The Entity Lifecycle system provides flexible change detection through multiple strategies:

1. **Snapshot Comparison**: Compare before/after JSON representations
2. **Event-Driven Detection**: Derive changes from domain events
3. **Manual Recording**: Explicit change recording for precise control

For detailed implementation patterns, see the [Implementation Guide](./implementation.md).

## Integration Overview

Each bounded context integrates with the Entity Lifecycle system by:

1. **Registering Entity Types**: Define how entities are serialized and tracked
2. **Implementing Change Detection**: Create entity-specific change detectors
3. **Wrapping Repositories**: Add version-aware capabilities to existing repositories

For complete integration patterns, see [Shared Kernel Usage](../integration-patterns/shared-kernel-usage.md).

## Next Steps

See related documentation:
- [Implementation Guide](./implementation.md) - Detailed implementation patterns and strategies
- [AI Agent System](../ai-agent/) - AI integration patterns
- [Shared Kernel Usage](../integration-patterns/shared-kernel-usage.md) - Context integration patterns
