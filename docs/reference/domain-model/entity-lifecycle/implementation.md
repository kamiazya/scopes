# Entity Lifecycle - Implementation Guide

This guide provides detailed implementation patterns for the Entity Lifecycle system, including change detection strategies, repository integration, and performance optimizations.

## Change Detection Strategies

The Entity Lifecycle system supports multiple strategies for detecting changes:

### 1. Interceptor-Based Detection

Automatically detect changes by intercepting entity operations.

```kotlin
interface EntityChangeInterceptor<T> {
    suspend fun beforeUpdate(entity: T): T
    suspend fun afterUpdate(before: T, after: T): List<EntityChange>
}

class AutomaticChangeInterceptor<T>(
    private val changeDetector: ChangeDetector<T>,
    private val changeService: EntityChangeService,
    private val currentUserProvider: CurrentUserProvider
) : EntityChangeInterceptor<T> {
    
    private val beforeSnapshots = mutableMapOf<EntityId, T>()
    
    override suspend fun beforeUpdate(entity: T): T {
        val entityId = extractEntityId(entity)
        beforeSnapshots[entityId] = entity
        return entity
    }
    
    override suspend fun afterUpdate(before: T, after: T): List<EntityChange> {
        val changes = changeDetector.detectChanges(before, after)
        
        // Record all changes atomically
        if (changes.isNotEmpty()) {
            changeService.recordChanges(changes).getOrElse { error ->
                logger.error("Failed to record changes for entity", error)
                return emptyList()
            }
        }
        
        return changes
    }
}
```

**Integration with Repository**:
```kotlin
class InterceptedScopeRepository(
    private val baseRepository: ScopeRepository,
    private val interceptor: EntityChangeInterceptor<Scope>
) : ScopeRepository {
    
    override suspend fun save(scope: Scope): Result<Scope, PersistenceError> {
        val existing = baseRepository.findById(scope.id).getOrNull()
        
        val prepared = interceptor.beforeUpdate(scope)
        val result = baseRepository.save(prepared)
        
        if (result.isSuccess && existing != null) {
            interceptor.afterUpdate(existing, prepared)
        }
        
        return result
    }
}
```

### 2. Event-Driven Detection

Detect changes from domain events as they occur.

```kotlin
@EventHandler
class ChangeTrackingEventHandler(
    private val changeService: EntityChangeService
) {
    
    @EventListener
    suspend fun handle(event: ScopeTitleChangedEvent) {
        val change = EntityChange(
            id = ChangeId.generate(),
            entityType = EntityType.SCOPE,
            entityId = EntityId(event.scopeId.value),
            fieldPath = FieldPath("title"),
            operation = ChangeOperation.UPDATE,
            beforeValue = event.oldTitle?.let { JsonPrimitive(it.value) },
            afterValue = JsonPrimitive(event.newTitle.value),
            version = event.version ?: VersionId.MAIN,
            causedBy = CausedBy.User(event.changedBy),
            timestamp = event.occurredAt
        )
        
        changeService.recordChange(change)
    }
    
    @EventListener 
    suspend fun handle(event: ScopeHierarchyChangedEvent) {
        val changes = mutableListOf<EntityChange>()
        
        // Track parent change
        if (event.oldParentId != event.newParentId) {
            changes += EntityChange(
                fieldPath = FieldPath("parentId"),
                operation = ChangeOperation.UPDATE,
                beforeValue = event.oldParentId?.let { JsonPrimitive(it.value) },
                afterValue = event.newParentId?.let { JsonPrimitive(it.value) },
                // ... other fields
            )
        }
        
        if (changes.isNotEmpty()) {
            changeService.recordChanges(changes)
        }
    }
}
```

### 3. Manual Change Recording

Explicit change recording in business logic for maximum control.

```kotlin
class ScopeManagementService(
    private val repository: ScopeRepository,
    private val changeService: EntityChangeService,
    private val currentUser: CurrentUserProvider
) {
    suspend fun updateScopeStatus(
        scopeId: ScopeId, 
        newStatus: ScopeStatus,
        reason: String? = null
    ): Result<Unit, ScopeError> {
        val scope = repository.findById(scopeId)
            .getOrElse { return Result.failure(ScopeError.NotFound(scopeId)) }
            
        val oldStatus = scope.status
        if (oldStatus == newStatus) {
            return Result.success(Unit) // No change needed
        }
        
        val updatedScope = scope.copy(status = newStatus)
        repository.save(updatedScope)
            .getOrElse { return Result.failure(ScopeError.PersistenceFailed(it)) }
        
        // Record the status change
        val change = EntityChange(
            id = ChangeId.generate(),
            entityType = EntityType.SCOPE,
            entityId = EntityId(scopeId.value),
            fieldPath = FieldPath("status"),
            operation = ChangeOperation.UPDATE,
            beforeValue = JsonPrimitive(oldStatus.name),
            afterValue = JsonPrimitive(newStatus.name),
            version = VersionId.MAIN,
            causedBy = CausedBy.User(currentUser.getUserId()),
            timestamp = Clock.System.now(),
            metadata = ChangeMetadata(
                reason = reason,
                tags = setOf("status-change", "manual")
            )
        )
        
        changeService.recordChange(change)
            .getOrElse { error ->
                logger.warn("Failed to record status change", error)
                // Don't fail the operation if change recording fails
            }
        
        return Result.success(Unit)
    }
}
```

## Change Detection Algorithms

### Snapshot Comparison

Compare JSON representations to detect field-level changes.

```kotlin
class JsonDiffChangeDetector : ChangeDetector<JsonObject> {
    
    override fun detectChanges(before: JsonObject?, after: JsonObject): List<EntityChange> {
        return when {
            before == null -> detectCreation(after)
            before == after -> emptyList() // No changes
            else -> detectModifications(before, after)
        }
    }
    
    private fun detectModifications(before: JsonObject, after: JsonObject): List<EntityChange> {
        val changes = mutableListOf<EntityChange>()
        val beforeFlat = flattenJson(before)
        val afterFlat = flattenJson(after)
        
        val allPaths = (beforeFlat.keys + afterFlat.keys).distinct()
        
        for (path in allPaths) {
            val beforeValue = beforeFlat[path]
            val afterValue = afterFlat[path]
            
            when {
                beforeValue != null && afterValue == null -> {
                    // Field deleted
                    changes += EntityChange(
                        fieldPath = FieldPath(path),
                        operation = ChangeOperation.DELETE,
                        beforeValue = beforeValue,
                        afterValue = null,
                        // ... other fields
                    )
                }
                beforeValue == null && afterValue != null -> {
                    // Field created
                    changes += EntityChange(
                        fieldPath = FieldPath(path),
                        operation = ChangeOperation.CREATE,
                        beforeValue = null,
                        afterValue = afterValue,
                        // ... other fields  
                    )
                }
                beforeValue != afterValue -> {
                    // Field modified
                    changes += EntityChange(
                        fieldPath = FieldPath(path),
                        operation = ChangeOperation.UPDATE,
                        beforeValue = beforeValue,
                        afterValue = afterValue,
                        // ... other fields
                    )
                }
            }
        }
        
        return changes
    }
    
    private fun flattenJson(obj: JsonObject, prefix: String = ""): Map<String, JsonElement> {
        val result = mutableMapOf<String, JsonElement>()
        
        for ((key, value) in obj) {
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            
            when (value) {
                is JsonObject -> result.putAll(flattenJson(value, path))
                is JsonArray -> {
                    value.forEachIndexed { index, element ->
                        when (element) {
                            is JsonObject -> result.putAll(flattenJson(element, "$path[$index]"))
                            else -> result["$path[$index]"] = element
                        }
                    }
                }
                else -> result[path] = value
            }
        }
        
        return result
    }
}
```

### Selective Field Tracking

Track only specific fields to reduce noise and improve performance.

```kotlin
class SelectiveChangeDetector<T>(
    private val trackedFields: Set<FieldPath>,
    private val serializer: KSerializer<T>
) : ChangeDetector<T> {
    
    override fun detectChanges(before: T?, after: T): List<EntityChange> {
        val beforeJson = before?.let { Json.encodeToJsonElement(serializer, it).jsonObject }
        val afterJson = Json.encodeToJsonElement(serializer, after).jsonObject
        
        val allChanges = JsonDiffChangeDetector().detectChanges(beforeJson, afterJson)
        
        // Filter to only tracked fields
        return allChanges.filter { change ->
            trackedFields.any { trackedField ->
                change.fieldPath.toString().startsWith(trackedField.toString())
            }
        }
    }
}

// Example: Only track specific Scope fields
val scopeChangeDetector = SelectiveChangeDetector(
    trackedFields = setOf(
        FieldPath("title"),
        FieldPath("status"), 
        FieldPath("parentId"),
        FieldPath("aspects.priority"),
        FieldPath("aspects.deadline")
    ),
    serializer = Scope.serializer()
)
```

## Repository Integration Patterns

### Version-Aware Repository

Enable loading entities from specific versions:

```kotlin
class VersionAwareScopeRepository(
    private val baseRepository: ScopeRepository,
    private val snapshotRepository: EntitySnapshotRepository
) : ScopeRepository {
    
    override suspend fun findById(id: ScopeId, version: VersionId?): Result<Scope?, QueryError> {
        return if (version == null) {
            baseRepository.findById(id)
        } else {
            snapshotRepository.findByVersion(EntityId(id.value), version)
                .map { snapshot -> 
                    snapshot?.data?.let { deserializeScope(it) }
                }
        }
    }
}
```

### Change Tracking Repository Wrapper

Automatically track changes for all repository operations:

```kotlin
class ChangeTrackingRepositoryWrapper<T, ID>(
    private val baseRepository: Repository<T, ID>,
    private val changeService: EntityChangeService,
    private val changeDetector: ChangeDetector<T>,
    private val entityType: EntityType,
    private val idExtractor: (T) -> EntityId
) : Repository<T, ID> {
    
    override suspend fun save(entity: T): Result<T, PersistenceError> {
        val entityId = idExtractor(entity)
        val existing = baseRepository.findById(extractId(entity)).getOrNull()
        
        val result = baseRepository.save(entity)
        
        if (result.isSuccess) {
            val changes = changeDetector.detectChanges(existing, entity)
            if (changes.isNotEmpty()) {
                changeService.recordChanges(changes).getOrElse { error ->
                    logger.warn("Failed to record changes for $entityType $entityId", error)
                }
            }
        }
        
        return result
    }
}
```

## Atomic Change Operations

Group related changes to ensure consistency.

```kotlin
class AtomicChangeService(
    private val repository: EntityChangeRepository,
    private val eventPublisher: DomainEventPublisher
) : EntityChangeService {
    
    override suspend fun recordChanges(changes: List<EntityChange>): Result<List<ChangeId>, ChangeRecordingError> {
        if (changes.isEmpty()) return Result.success(emptyList())
        
        // Validate all changes belong to same entity and version
        val entityId = changes.first().entityId
        val version = changes.first().version
        val causedBy = changes.first().causedBy
        val timestamp = Clock.System.now()
        
        val normalizedChanges = changes.map { change ->
            require(change.entityId == entityId) { "All changes must be for same entity" }
            require(change.version == version) { "All changes must be for same version" }
            
            change.copy(
                timestamp = timestamp, // Ensure all changes have same timestamp
                causedBy = causedBy // Ensure consistent attribution
            )
        }
        
        // Persist all changes atomically
        return repository.saveAll(normalizedChanges).map { savedChanges ->
            // Publish single event for all changes
            val event = EntityChangedEvent(
                entityType = savedChanges.first().entityType,
                entityId = entityId,
                version = version,
                causedBy = causedBy,
                changes = savedChanges,
                occurredAt = timestamp
            )
            
            eventPublisher.publish(event)
            
            savedChanges.map { it.id }
        }
    }
}
```

## Performance Optimizations

### Change Batching

Batch multiple changes to reduce database load.

```kotlin
class BatchingChangeService(
    private val baseService: EntityChangeService,
    private val batchSize: Int = 100,
    private val flushInterval: Duration = 5.seconds
) : EntityChangeService {
    
    private val pendingChanges = mutableMapOf<EntityId, MutableList<EntityChange>>()
    private val batchTimer = Timer()
    
    init {
        // Periodic flush
        batchTimer.scheduleAtFixedRate(0, flushInterval.inWholeMilliseconds) {
            flushPendingChanges()
        }
    }
    
    override suspend fun recordChange(change: EntityChange): Result<ChangeId, ChangeRecordingError> {
        synchronized(pendingChanges) {
            pendingChanges.getOrPut(change.entityId) { mutableListOf() }.add(change)
            
            val entityChanges = pendingChanges[change.entityId]!!
            if (entityChanges.size >= batchSize) {
                flushEntityChanges(change.entityId, entityChanges.toList())
                entityChanges.clear()
            }
        }
        
        return Result.success(change.id)
    }
    
    private suspend fun flushPendingChanges() {
        val toFlush = synchronized(pendingChanges) {
            val copy = pendingChanges.toMap()
            pendingChanges.clear()
            copy
        }
        
        for ((entityId, changes) in toFlush) {
            if (changes.isNotEmpty()) {
                flushEntityChanges(entityId, changes)
            }
        }
    }
}
```

### Change Deduplication

Avoid recording duplicate changes.

```kotlin
class DeduplicatingChangeService(
    private val baseService: EntityChangeService,
    private val recentChangesCache: Cache<String, EntityChange>
) : EntityChangeService {
    
    override suspend fun recordChange(change: EntityChange): Result<ChangeId, ChangeRecordingError> {
        val changeKey = buildChangeKey(change)
        val existingChange = recentChangesCache.get(changeKey)
        
        if (existingChange != null && isSameChange(existingChange, change)) {
            logger.debug("Skipping duplicate change for ${change.entityId}:${change.fieldPath}")
            return Result.success(existingChange.id)
        }
        
        return baseService.recordChange(change).onSuccess { changeId ->
            recentChangesCache.put(changeKey, change)
        }
    }
    
    private fun buildChangeKey(change: EntityChange): String {
        val valueHash = change.afterValue.hashCode()
        return "${change.entityId.value}:${change.fieldPath}:$valueHash"
    }
}
```

### Async Processing

Process changes asynchronously for better responsiveness:

```kotlin
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

### Caching Strategies

Cache frequently accessed snapshots:

```kotlin
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

## Error Handling and Recovery

### Change Recording Failures

```kotlin
sealed class ChangeRecordingError {
    data class ValidationError(val message: String) : ChangeRecordingError()
    data class PersistenceError(val cause: Throwable) : ChangeRecordingError()
    data class SerializationError(val cause: Throwable) : ChangeRecordingError()
    data class DuplicateError(val existingChangeId: ChangeId) : ChangeRecordingError()
}

class ResilientChangeService(
    private val baseService: EntityChangeService,
    private val retryPolicy: RetryPolicy = RetryPolicy.exponentialBackoff(maxRetries = 3)
) : EntityChangeService {
    
    override suspend fun recordChange(change: EntityChange): Result<ChangeId, ChangeRecordingError> {
        return retryPolicy.execute {
            baseService.recordChange(change)
        }.recoverCatching { error ->
            when (error) {
                is ChangeRecordingError.ValidationError -> {
                    // Don't retry validation errors
                    throw error
                }
                is ChangeRecordingError.PersistenceError -> {
                    // Log and potentially store for later retry
                    logger.warn("Change recording failed, will retry", error)
                    throw error
                }
                else -> throw error
            }
        }
    }
}
```

## Monitoring and Observability

### Change Metrics

```kotlin
class MetricsAwareChangeService(
    private val baseService: EntityChangeService,
    private val meterRegistry: MeterRegistry
) : EntityChangeService {
    
    private val changeCounter = Counter.builder("entity.changes.recorded")
        .tag("type", "entity_change")
        .register(meterRegistry)
        
    private val changeTimer = Timer.builder("entity.changes.recording_time")
        .register(meterRegistry)
    
    override suspend fun recordChange(change: EntityChange): Result<ChangeId, ChangeRecordingError> {
        return Timer.Sample.start(meterRegistry).use { sample ->
            baseService.recordChange(change).onSuccess { 
                changeCounter.increment(
                    Tags.of(
                        "entity_type", change.entityType.value,
                        "operation", change.operation.name,
                        "caused_by", change.causedBy::class.simpleName ?: "Unknown"
                    )
                )
                sample.stop(changeTimer)
            }
        }
    }
}
```

## Testing Strategies

### Change Detection Testing

```kotlin
class ChangeDetectionTest {
    private val changeDetector = JsonDiffChangeDetector()
    
    @Test
    fun `should detect title change`() {
        val before = JsonObject(mapOf(
            "id" to JsonPrimitive("scope-1"),
            "title" to JsonPrimitive("Old Title"),
            "status" to JsonPrimitive("ACTIVE")
        ))
        
        val after = before.toMutableMap().apply {
            put("title", JsonPrimitive("New Title"))
        }.let { JsonObject(it) }
        
        val changes = changeDetector.detectChanges(before, after)
        
        changes shouldHaveSize 1
        changes[0].apply {
            fieldPath shouldBe FieldPath("title")
            operation shouldBe ChangeOperation.UPDATE
            beforeValue shouldBe JsonPrimitive("Old Title")
            afterValue shouldBe JsonPrimitive("New Title")
        }
    }
    
    @Test
    fun `should detect nested property changes`() {
        val before = JsonObject(mapOf(
            "id" to JsonPrimitive("scope-1"),
            "aspects" to JsonObject(mapOf(
                "priority" to JsonPrimitive("HIGH"),
                "deadline" to JsonPrimitive("2025-01-31")
            ))
        ))
        
        val after = before.toMutableMap().apply {
            put("aspects", JsonObject(mapOf(
                "priority" to JsonPrimitive("MEDIUM"), // Changed
                "deadline" to JsonPrimitive("2025-01-31") // Unchanged
            )))
        }.let { JsonObject(it) }
        
        val changes = changeDetector.detectChanges(before, after)
        
        changes shouldHaveSize 1
        changes[0].fieldPath shouldBe FieldPath("aspects.priority")
    }
}
```

## Database Schema

### Events Table
```sql
CREATE TABLE entity_lifecycle_events (
    event_id TEXT PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    version_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    caused_by_type TEXT NOT NULL,
    caused_by_id TEXT,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    INDEX idx_events_entity (entity_type, entity_id),
    INDEX idx_events_version (version_id),
    INDEX idx_events_occurred (occurred_at),
    INDEX idx_events_type (event_type)
);
```

### Snapshots Table
```sql
CREATE TABLE entity_snapshots (
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    version_id TEXT NOT NULL,
    event_id TEXT NOT NULL, -- Last event before snapshot
    snapshot_data JSONB NOT NULL,
    checksum TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    PRIMARY KEY (entity_type, entity_id, version_id),
    INDEX idx_snapshots_event (event_id)
);
```

## Next Steps

- [Core Concepts](./core-concepts.md) - Understand the fundamental concepts
- [AI Agent System](../ai-agent/) - Integrate AI capabilities
- [Shared Kernel Usage](../integration-patterns/shared-kernel-usage.md) - Context integration patterns
