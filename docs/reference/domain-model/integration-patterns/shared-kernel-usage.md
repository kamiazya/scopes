# Integration Patterns: Shared Kernel Usage

This document provides comprehensive guidance on how to use the Entity Lifecycle Shared Kernel across multiple Bounded Contexts in the Scopes system, including patterns, best practices, and common pitfalls.

## Overview

The Entity Lifecycle functionality is implemented as a **Shared Kernel** pattern, where multiple Bounded Contexts directly share the same change management domain model. This approach provides consistency and performance while requiring careful coordination.

## Shared Kernel Architecture

### What's in the Shared Kernel

#### Core Value Objects
```kotlin
// Shared across ALL contexts using Entity Lifecycle
package io.github.kamiazya.scopes.shared.entitylifecycle.domain.valueobject

data class EntityType(val value: String)
data class EntityId(val value: String)  
data class FieldPath(val segments: List<String>)
data class VersionId(val value: String)
data class ChangeId(val value: String)
enum class ChangeOperation { CREATE, UPDATE, DELETE }
sealed class CausedBy {
    data class User(val userId: UserId) : CausedBy()
    data class AIAgent(val agentId: AgentId) : CausedBy()
    object System : CausedBy()
}
```

#### Domain Entities
```kotlin
// Shared entity definitions
package io.github.kamiazya.scopes.shared.entitylifecycle.domain.entity

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

data class EntitySnapshot(
    val entityType: EntityType,
    val entityId: EntityId,
    val version: VersionId,
    val timestamp: Instant,
    val data: JsonObject,
    val checksum: ChecksumValue,
    val metadata: SnapshotMetadata = SnapshotMetadata()
)
```

#### Service Interfaces
```kotlin
// Shared service contracts
package io.github.kamiazya.scopes.shared.entitylifecycle.domain.service

interface EntityChangeService {
    suspend fun recordChange(change: EntityChange): Result<ChangeId, ChangeRecordingError>
    suspend fun recordChanges(changes: List<EntityChange>): Result<List<ChangeId>, ChangeRecordingError>
    suspend fun getChanges(entityId: EntityId, since: Instant? = null, version: VersionId? = null): Result<List<EntityChange>, QueryError>
}

interface EntityVersionService {
    suspend fun createVersion(request: CreateVersionRequest): Result<EntityVersion, VersionCreationError>
    suspend fun switchVersion(entityId: EntityId, targetVersion: VersionId): Result<EntitySnapshot, SwitchError>
    suspend fun mergeVersion(sourceVersion: VersionId, targetVersion: VersionId, strategy: MergeStrategy = MergeStrategy.AUTOMATIC): Result<MergeResult, MergeError>
}

interface EntitySnapshotService {
    suspend fun createSnapshot(entityId: EntityId, version: VersionId): Result<EntitySnapshot, SnapshotError>
    suspend fun getSnapshot(entityId: EntityId, version: VersionId): Result<EntitySnapshot?, QueryError>
}
```

### What's Context-Specific

Each Bounded Context maintains its own:
- **Repository Implementations**: Database schemas and persistence logic
- **Entity Serialization**: How to convert entities to/from JSON
- **Change Detection Logic**: How to detect changes for specific entity types
- **Business Rules**: Entity-specific validation and constraints
- **UI Integration**: How changes are presented to users

## Context Integration Patterns

### 1. Repository Integration Pattern

Wrap existing repositories with change detection capabilities:

```kotlin
// Example: Scope Management Context
class ChangeTrackingScopeRepository(
    private val baseRepository: ScopeRepository,
    private val changeService: EntityChangeService,
    private val changeDetector: ScopeChangeDetector
) : ScopeRepository {
    
    override suspend fun save(scope: Scope): Result<Scope, PersistenceError> {
        val existing = baseRepository.findById(scope.id).getOrNull()
        
        // Perform the save operation
        val saveResult = baseRepository.save(scope)
        
        if (saveResult.isSuccess) {
            // Detect and record changes asynchronously
            launch(Dispatchers.IO) {
                val changes = changeDetector.detectChanges(existing, scope)
                if (changes.isNotEmpty()) {
                    changeService.recordChanges(changes).onFailure { error ->
                        logger.warn("Failed to record changes for Scope ${scope.id}", error)
                    }
                }
            }
        }
        
        return saveResult
    }
    
    // Version-aware loading
    override suspend fun findById(id: ScopeId, version: VersionId?): Result<Scope?, QueryError> {
        return if (version == null || version == VersionId.MAIN) {
            baseRepository.findById(id)
        } else {
            loadFromSnapshot(EntityId(id.value), version)
        }
    }
    
    private suspend fun loadFromSnapshot(entityId: EntityId, version: VersionId): Result<Scope?, QueryError> {
        return snapshotService.getSnapshot(entityId, version)
            .mapCatching { snapshot ->
                snapshot?.let { deserializeScope(it.data) }
            }
            .mapError { QueryError.SnapshotLoadFailed(it) }
    }
}
```

### 2. Change Detector Implementation Pattern

Implement entity-specific change detection:

```kotlin
// Each context implements its own change detector
class ScopeChangeDetector : ChangeDetector<Scope> {
    
    override fun detectChanges(before: Scope?, after: Scope): List<EntityChange> {
        val changes = mutableListOf<EntityChange>()
        val entityId = EntityId(after.id.value)
        val timestamp = Clock.System.now()
        val causedBy = getCurrentCausedBy()
        
        if (before == null) {
            // Entity creation - use creation detector
            return detectCreation(after, entityId, causedBy, timestamp)
        }
        
        // Field-by-field comparison
        if (before.title != after.title) {
            changes += EntityChange(
                id = ChangeId.generate(),
                entityType = EntityType.SCOPE,
                entityId = entityId,
                fieldPath = FieldPath("title"),
                operation = ChangeOperation.UPDATE,
                beforeValue = JsonPrimitive(before.title.value),
                afterValue = JsonPrimitive(after.title.value),
                version = VersionId.MAIN,
                causedBy = causedBy,
                timestamp = timestamp
            )
        }
        
        // Complex field comparisons
        detectStatusChanges(before, after, changes, entityId, causedBy, timestamp)
        detectHierarchyChanges(before, after, changes, entityId, causedBy, timestamp)
        detectAspectChanges(before, after, changes, entityId, causedBy, timestamp)
        
        return changes
    }
    
    private fun detectStatusChanges(
        before: Scope,
        after: Scope,
        changes: MutableList<EntityChange>,
        entityId: EntityId,
        causedBy: CausedBy,
        timestamp: Instant
    ) {
        if (before.status != after.status) {
            changes += EntityChange(
                id = ChangeId.generate(),
                entityType = EntityType.SCOPE,
                entityId = entityId,
                fieldPath = FieldPath("status"),
                operation = ChangeOperation.UPDATE,
                beforeValue = JsonPrimitive(before.status.name),
                afterValue = JsonPrimitive(after.status.name),
                version = VersionId.MAIN,
                causedBy = causedBy,
                timestamp = timestamp,
                metadata = ChangeMetadata(
                    reason = "Status transition: ${before.status} → ${after.status}",
                    tags = setOf("status-change", "lifecycle")
                )
            )
        }
    }
}
```

### 3. Entity Registration Pattern

Register entity types with the shared kernel:

```kotlin
// Context-specific registration
@Component
class ScopeEntityLifecycleBootstrap(
    private val entityLifecycleRegistry: EntityLifecycleRegistry,
    private val scopeChangeDetector: ScopeChangeDetector
) {
    
    @PostConstruct
    fun registerScopeEntity() {
        entityLifecycleRegistry.register(
            EntityTypeRegistration(
                entityType = EntityType.SCOPE,
                serializer = { scope: Scope -> Json.encodeToJsonElement(scope).jsonObject },
                deserializer = { json: JsonObject -> Json.decodeFromJsonElement<Scope>(json) },
                changeDetector = scopeChangeDetector,
                idExtractor = { scope: Scope -> EntityId(scope.id.value) },
                versionExtractor = { _: Scope -> VersionId.MAIN }, // Or implement versioning
                contextName = "scope-management"
            )
        )
    }
}
```

### 4. Event Integration Pattern

Connect shared kernel events with context-specific events:

```kotlin
// Event translator from shared kernel to context events
class ScopeLifecycleEventTranslator(
    private val domainEventPublisher: DomainEventPublisher
) {
    
    @EventListener
    suspend fun handle(event: EntityChangedEvent) {
        if (event.entityType != EntityType.SCOPE) return
        
        val scopeId = ScopeId(event.entityId.value)
        
        // Translate to context-specific events
        event.changes.forEach { change ->
            when (change.fieldPath.toString()) {
                "title" -> {
                    domainEventPublisher.publish(
                        ScopeTitleChangedEvent(
                            scopeId = scopeId,
                            oldTitle = change.beforeValue?.toString(),
                            newTitle = change.afterValue.toString(),
                            changedBy = translateCausedBy(change.causedBy),
                            occurredAt = change.timestamp
                        )
                    )
                }
                
                "status" -> {
                    domainEventPublisher.publish(
                        ScopeStatusChangedEvent(
                            scopeId = scopeId,
                            oldStatus = change.beforeValue?.let { ScopeStatus.valueOf(it.toString()) },
                            newStatus = ScopeStatus.valueOf(change.afterValue.toString()),
                            changedBy = translateCausedBy(change.causedBy),
                            occurredAt = change.timestamp
                        )
                    )
                }
                
                "parentId" -> {
                    domainEventPublisher.publish(
                        ScopeHierarchyChangedEvent(
                            scopeId = scopeId,
                            oldParentId = change.beforeValue?.let { ScopeId(it.toString()) },
                            newParentId = change.afterValue?.let { ScopeId(it.toString()) },
                            changedBy = translateCausedBy(change.causedBy),
                            occurredAt = change.timestamp
                        )
                    )
                }
            }
        }
    }
}
```

### 5. Version-Aware Query Pattern

Enable querying entities at specific versions:

```kotlin
// Version-aware query service
class VersionAwareScopeQueryService(
    private val baseRepository: ScopeRepository,
    private val snapshotService: EntitySnapshotService,
    private val changeService: EntityChangeService
) {
    
    suspend fun findScopeAtVersion(
        scopeId: ScopeId,
        version: VersionId
    ): Result<Scope?, QueryError> {
        // Try snapshot first for performance
        val snapshot = snapshotService.getSnapshot(EntityId(scopeId.value), version)
            .getOrNull()
        
        if (snapshot != null) {
            return Result.success(deserializeScope(snapshot.data))
        }
        
        // Fallback to event replay
        return reconstructScopeFromChanges(scopeId, version)
    }
    
    suspend fun getScopeHistory(
        scopeId: ScopeId,
        limit: Int = 50
    ): Result<List<ScopeHistoryEntry>, QueryError> {
        val changes = changeService.getChanges(EntityId(scopeId.value))
            .getOrElse { return Result.failure(it) }
        
        return Result.success(
            changes.takeLast(limit).map { change ->
                ScopeHistoryEntry(
                    changeId = change.id,
                    field = change.fieldPath.toString(),
                    operation = change.operation,
                    oldValue = change.beforeValue?.toString(),
                    newValue = change.afterValue?.toString(),
                    changedBy = change.causedBy,
                    changedAt = change.timestamp,
                    reason = change.metadata.reason
                )
            }
        )
    }
}
```

## Dependency Injection Configuration

### Shared Kernel Module

```kotlin
@Module
class SharedEntityLifecycleModule {
    
    @Provides
    @Singleton
    fun provideEntityLifecycleRegistry(): EntityLifecycleRegistry = 
        InMemoryEntityLifecycleRegistry()
    
    @Provides
    @Singleton
    fun provideEntityChangeService(
        changeRepository: EntityChangeRepository,
        eventPublisher: DomainEventPublisher
    ): EntityChangeService = 
        DefaultEntityChangeService(changeRepository, eventPublisher)
    
    @Provides
    @Singleton  
    fun provideEntityVersionService(
        versionRepository: EntityVersionRepository,
        snapshotService: EntitySnapshotService
    ): EntityVersionService = 
        DefaultEntityVersionService(versionRepository, snapshotService)
    
    @Provides
    @Singleton
    fun provideEntitySnapshotService(
        snapshotRepository: EntitySnapshotRepository
    ): EntitySnapshotService = 
        DefaultEntitySnapshotService(snapshotRepository)
}
```

### Context-Specific Module

```kotlin
@Module
class ScopeManagementLifecycleModule {
    
    @Provides
    @Singleton
    fun provideScopeChangeDetector(): ScopeChangeDetector = 
        ScopeChangeDetector()
    
    @Provides
    @Singleton
    fun provideChangeTrackingScopeRepository(
        baseRepository: ScopeRepository,
        changeService: EntityChangeService,
        changeDetector: ScopeChangeDetector
    ): ScopeRepository = 
        ChangeTrackingScopeRepository(baseRepository, changeService, changeDetector)
    
    @Provides
    @Singleton
    fun provideVersionAwareScopeQueryService(
        baseRepository: ScopeRepository,
        snapshotService: EntitySnapshotService,
        changeService: EntityChangeService
    ): VersionAwareScopeQueryService = 
        VersionAwareScopeQueryService(baseRepository, snapshotService, changeService)
    
    @Provides
    @IntoSet
    fun provideScopeLifecycleBootstrap(
        registry: EntityLifecycleRegistry,
        changeDetector: ScopeChangeDetector
    ): EntityLifecycleBootstrap = 
        ScopeEntityLifecycleBootstrap(registry, changeDetector)
}
```

## Testing Shared Kernel Integration

### Unit Testing

```kotlin
class ScopeChangeDetectorTest {
    private val changeDetector = ScopeChangeDetector()
    
    @Test
    fun `should detect title change`() {
        val original = createSampleScope(title = "Original Title")
        val updated = original.copy(title = ScopeTitle("Updated Title"))
        
        val changes = changeDetector.detectChanges(original, updated)
        
        changes shouldHaveSize 1
        changes[0].apply {
            entityType shouldBe EntityType.SCOPE
            fieldPath shouldBe FieldPath("title")
            operation shouldBe ChangeOperation.UPDATE
            beforeValue shouldBe JsonPrimitive("Original Title")
            afterValue shouldBe JsonPrimitive("Updated Title")
        }
    }
    
    @Test
    fun `should detect complex hierarchy changes`() {
        val original = createSampleScope(parentId = null)
        val parent = ScopeId.generate()
        val updated = original.copy(parentId = parent)
        
        val changes = changeDetector.detectChanges(original, updated)
        
        val hierarchyChange = changes.find { it.fieldPath == FieldPath("parentId") }
        hierarchyChange shouldNotBe null
        hierarchyChange!!.apply {
            operation shouldBe ChangeOperation.UPDATE
            beforeValue shouldBe null
            afterValue shouldBe JsonPrimitive(parent.value)
        }
    }
}
```

### Integration Testing

```kotlin
@IntegrationTest
class ScopeLifecycleIntegrationTest {
    
    @Autowired
    private lateinit var scopeRepository: ScopeRepository
    
    @Autowired
    private lateinit var changeService: EntityChangeService
    
    @Autowired
    private lateinit var versionService: EntityVersionService
    
    @Test
    fun `should track changes and enable versioning`() = runTest {
        // Create scope
        val scope = createSampleScope()
        scopeRepository.save(scope).getOrThrow()
        
        // Create a version
        val version = versionService.createVersion(
            CreateVersionRequest(
                entityId = EntityId(scope.id.value),
                parentVersion = VersionId.MAIN,
                name = VersionName("feature-branch")
            )
        ).getOrThrow()
        
        // Modify scope in the version
        val updated = scope.copy(title = ScopeTitle("Updated in Branch"))
        // Note: This would require version-aware save logic
        
        // Verify changes were tracked
        val changes = changeService.getChanges(
            EntityId(scope.id.value),
            version = version.id
        ).getOrThrow()
        
        changes shouldNotBe empty
        changes.any { it.fieldPath == FieldPath("title") } shouldBe true
    }
}
```

## Best Practices

### 1. Change Detection

**Do:**
- ✅ Implement efficient field-by-field comparison
- ✅ Use meaningful change metadata (reasons, tags)
- ✅ Batch related changes together
- ✅ Handle null/creation cases properly

**Don't:**
- ❌ Detect changes for every field unnecessarily
- ❌ Forget to handle complex object comparisons  
- ❌ Skip change detection for important operations
- ❌ Block operations if change recording fails

### 2. Entity Registration

**Do:**
- ✅ Register entities during application bootstrap
- ✅ Provide clear entity type names
- ✅ Implement proper serialization/deserialization
- ✅ Test serialization round-trips

**Don't:**
- ❌ Register the same entity type multiple times
- ❌ Use generic or ambiguous entity type names
- ❌ Forget to handle serialization errors
- ❌ Change entity type names in production

### 3. Event Integration

**Do:**
- ✅ Translate shared events to context-specific events
- ✅ Maintain event ordering and causality
- ✅ Handle event processing failures gracefully
- ✅ Provide clear event documentation

**Don't:**
- ❌ Directly expose shared kernel events to UI
- ❌ Create circular event dependencies
- ❌ Lose important event information in translation
- ❌ Forget to handle async event processing

### 4. Error Handling

**Do:**
- ✅ Use Result types for error handling
- ✅ Provide specific error types and messages
- ✅ Log errors with sufficient context
- ✅ Implement retry logic for transient failures

**Don't:**
- ❌ Fail operations if change recording fails
- ❌ Swallow exceptions without logging
- ❌ Use generic error types
- ❌ Expose internal errors to users

## Performance Considerations

### 1. Change Detection Optimization

```kotlin
// Selective field tracking
class OptimizedScopeChangeDetector(
    private val trackedFields: Set<FieldPath> = setOf(
        FieldPath("title"),
        FieldPath("status"),
        FieldPath("parentId"),
        FieldPath("aspects.priority"),
        FieldPath("aspects.deadline")
    )
) : ChangeDetector<Scope> {
    
    override fun detectChanges(before: Scope?, after: Scope): List<EntityChange> {
        val allChanges = fullDetection(before, after)
        
        // Filter to only tracked fields
        return allChanges.filter { change ->
            trackedFields.any { trackedField ->
                change.fieldPath.toString().startsWith(trackedField.toString())
            }
        }
    }
}
```

### 2. Batching and Async Processing

```kotlin
// Async change recording
class AsyncChangeTrackingScopeRepository(
    private val baseRepository: ScopeRepository,
    private val changeService: EntityChangeService,
    private val changeDetector: ScopeChangeDetector,
    private val backgroundScope: CoroutineScope
) : ScopeRepository {
    
    override suspend fun save(scope: Scope): Result<Scope, PersistenceError> {
        val existing = baseRepository.findById(scope.id).getOrNull()
        val result = baseRepository.save(scope)
        
        if (result.isSuccess) {
            // Record changes asynchronously
            backgroundScope.launch {
                try {
                    val changes = changeDetector.detectChanges(existing, scope)
                    if (changes.isNotEmpty()) {
                        changeService.recordChanges(changes)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to record changes for Scope ${scope.id}", e)
                }
            }
        }
        
        return result
    }
}
```

### 3. Caching Strategies

```kotlin
// Cached snapshot service
class CachedEntitySnapshotService(
    private val baseService: EntitySnapshotService,
    private val cache: Cache<String, EntitySnapshot>
) : EntitySnapshotService {
    
    override suspend fun getSnapshot(
        entityId: EntityId,
        version: VersionId
    ): Result<EntitySnapshot?, QueryError> {
        val cacheKey = "${entityId.value}:${version.value}"
        
        cache.get(cacheKey)?.let { cachedSnapshot ->
            return Result.success(cachedSnapshot)
        }
        
        return baseService.getSnapshot(entityId, version).onSuccess { snapshot ->
            snapshot?.let { cache.put(cacheKey, it) }
        }
    }
}
```

## Common Pitfalls and Solutions

### 1. Circular Dependencies

**Problem:**
```kotlin
// ❌ Context depends on shared kernel, shared kernel depends on context
class SharedKernelService(
    private val scopeRepository: ScopeRepository // Context dependency!
)
```

**Solution:**
```kotlin
// ✅ Use dependency inversion
interface EntityRepository<T, ID> {
    suspend fun findById(id: ID): Result<T?, QueryError>
}

class SharedKernelService(
    private val entityRepositoryProvider: (EntityType) -> EntityRepository<*, *>
)
```

### 2. Version Conflicts

**Problem:** Different contexts using different versions of shared kernel.

**Solution:**
```kotlin
// Use explicit version compatibility checks
@Component
class SharedKernelVersionValidator {
    
    @PostConstruct
    fun validateVersions() {
        val requiredVersion = SemanticVersion("1.2.0")
        val actualVersion = getSharedKernelVersion()
        
        require(actualVersion.isCompatibleWith(requiredVersion)) {
            "Incompatible shared kernel version: required $requiredVersion, actual $actualVersion"
        }
    }
}
```

### 3. Serialization Issues

**Problem:** Entity changes between versions break serialization.

**Solution:**
```kotlin
// Use versioned serialization
@Serializable
data class Scope(
    val id: ScopeId,
    val title: ScopeTitle,
    // ...other fields
) {
    companion object {
        fun serializer(version: Int = CURRENT_VERSION): KSerializer<Scope> {
            return when (version) {
                1 -> ScopeV1.serializer().map(
                    serialize = { it.toV1() },
                    deserialize = { it.toCurrentVersion() }
                )
                2 -> Scope.serializer() // Current version
                else -> throw IllegalArgumentException("Unsupported version: $version")
            }
        }
    }
}
```

## Migration Strategies

### Adding Shared Kernel to Existing Context

1. **Create Integration Layer**
2. **Implement Change Detection**
3. **Wire Up Events**
4. **Test Thoroughly**
5. **Deploy Incrementally**

### Removing Context from Shared Kernel

1. **Create Alternative Implementation**
2. **Migrate Data**
3. **Update Dependencies**
4. **Remove Integration Code**

## Next Steps

- [Entity Lifecycle Core Concepts](../entity-lifecycle/core-concepts.md)
- [Implementation Guide: Adding New Entity Types](../../../../tmp/adding-new-entity-types.md)
- [AI Agent System](../ai-agent/)
