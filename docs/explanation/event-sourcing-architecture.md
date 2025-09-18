# Event Sourcing Architecture (Foundation Only)

> ⚠️ **Implementation Status**: This is foundational infrastructure only. No user-facing features currently utilize event sourcing. The implementation exists to support future features like audit trails, sync, and time-travel debugging.

## Overview

Scopes includes the infrastructure for Event Sourcing pattern that will eventually capture all state changes as an immutable sequence of events. This architecture foundation is implemented but not yet exposed to users through CLI commands or other interfaces.

## Core Concepts

### Event Store Bounded Context

The Event Store is implemented as a dedicated bounded context (`contexts/event-store/`) that provides:

- **Event Persistence**: All domain events are persisted in an append-only log
- **Event Replay**: Ability to reconstruct state from any point in time
- **Event Publishing**: Asynchronous event distribution to subscribers
- **Aggregate Tracking**: Events are organized by aggregate roots

### Event Types

Events in Scopes follow a standardized structure:

```kotlin
interface DomainEvent {
    val aggregateId: String
    val occurredAt: Instant
    val version: Long
}
```

Common event types include:
- `ScopeCreatedEvent`
- `ScopeUpdatedEvent`
- `AliasAddedEvent`
- `AspectDefinedEvent`
- `ContextViewCreatedEvent`

## Architecture Components

### 1. Command Handlers
Command handlers validate business rules and emit events:

```kotlin
class CreateScopeHandler {
    suspend fun handle(command: CreateScopeCommand): Either<Error, ScopeCreatedEvent> {
        // Validate command
        // Generate event
        // Store in event store
    }
}
```

### 2. Event Store Infrastructure

Located in `contexts/event-store/infrastructure/`, the implementation provides:

- **SQLite-based persistence** with optimized append-only writes
- **Event serialization** using Kotlin serialization
- **Event querying** by aggregate, time range, or event type
- **Transaction support** for consistency guarantees

### 3. Event Publishers

The system supports multiple event publishing strategies:

- **In-Memory**: For testing and development
- **Database-backed**: Using SQLite for reliability
- **Future**: Message queue integration (Kafka, RabbitMQ)

### 4. Event Replay and Projection

```kotlin
class EventProjector {
    suspend fun projectEvents(
        events: Flow<DomainEvent>
    ): AggregateState {
        return events.fold(initialState) { state, event ->
            when (event) {
                is ScopeCreatedEvent -> state.handleCreated(event)
                is ScopeUpdatedEvent -> state.handleUpdated(event)
                // ... other event types
            }
        }
    }
}
```

## Planned Benefits (Not Yet Available)

> 🚧 **Note**: These are planned features that the event sourcing infrastructure will enable in the future. They are not currently available to users.

### 1. Complete Audit Trail (Future)
Will record every system change as an event, providing:
- Who made the change
- When it occurred
- What was changed
- Why (through command metadata)

### 2. Time-Travel Capabilities (Future)
Will allow reconstructing the exact state of any scope at any point in time:
```bash
# Planned future CLI command (not yet implemented)
scopes history <scope-id> --at "2025-01-15T10:30:00Z"
```

### 3. Distributed System Support (Future)
Will enable:
- **Device Synchronization**: Sync events between devices
- **Offline Support**: Queue events locally, sync when online
- **Conflict Resolution**: Use event timestamps for ordering

### 4. Analytics and Insights (Future)
Event streams will provide rich data for:
- Productivity metrics
- Workflow analysis
- AI training data

## Integration with Other Bounded Contexts

### Scope Management
- Emits events for all scope operations
- Subscribes to events for maintaining read models

### User Preferences
- Events track preference changes
- Enables preference history and rollback

### Device Synchronization
- Uses events as the synchronization protocol
- Implements vector clocks for distributed consistency

## Implementation Details

### Event Storage Schema

```sql
CREATE TABLE events (
    id TEXT PRIMARY KEY,
    aggregate_id TEXT NOT NULL,
    aggregate_type TEXT NOT NULL,
    event_type TEXT NOT NULL,
    event_data TEXT NOT NULL,  -- JSON serialized
    event_metadata TEXT,        -- Optional metadata
    occurred_at INTEGER NOT NULL,
    version INTEGER NOT NULL,

    INDEX idx_aggregate (aggregate_id, version),
    INDEX idx_occurred_at (occurred_at)
);
```

### Event Processing Pipeline

```
Command → Validation → Event Generation → Storage → Publishing → Projection
                                              ↓
                                     Event Subscribers
```

## Performance Considerations

### Write Performance
- **Append-only writes**: O(1) insertion time
- **Batch processing**: Group events for efficiency
- **Async publishing**: Non-blocking event distribution

### Read Performance
- **Snapshots**: Periodic state snapshots to avoid full replay
- **Caching**: In-memory caches for frequently accessed aggregates
- **Indexing**: Strategic indexes on event attributes

## Future Enhancements

### Near-term
- Event versioning and schema evolution
- Snapshot optimization strategies
- Event compression

### Long-term
- Distributed event store with sharding
- CQRS read model separation
- Event streaming API for real-time updates

## Testing Strategy

### Unit Tests
- Event handler logic
- Serialization/deserialization
- Business rule validation

### Integration Tests
- End-to-end event flow
- Database persistence
- Event ordering guarantees

### Property-Based Tests
- Event invariants
- Idempotency properties
- Consistency guarantees

## Developer Guidelines

### Creating New Events

1. Define event in domain layer:
```kotlin
data class MyNewEvent(
    override val aggregateId: String,
    override val occurredAt: Instant,
    override val version: Long,
    // Event-specific fields
    val specificData: String
) : DomainEvent
```

2. Register event type:
```kotlin
// In EventTypeRegistrar
EventTypeRegistry.register<MyNewEvent>("MyNewEvent")
```

3. Implement event handler:
```kotlin
class MyEventHandler {
    suspend fun handle(event: MyNewEvent) {
        // Process event
    }
}
```

### Best Practices

1. **Event Naming**: Use past tense (e.g., `ScopeCreated`, not `CreateScope`)
2. **Event Size**: Keep events small and focused
3. **Event Immutability**: Never modify stored events
4. **Event Ordering**: Rely on version numbers within aggregates
5. **Event Privacy**: Don't include sensitive data in events

## Related Documentation

- [Domain-Driven Design](./domain-driven-design.md) - DDD concepts including aggregates
- [Clean Architecture](./clean-architecture.md) - Architectural layers
- [Device Synchronization](./device-synchronization.md) - How events enable sync
- [Architecture Decision Records](./adr/0015-unified-event-sourcing-approach.md) - Event sourcing decisions