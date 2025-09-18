# Device Synchronization Architecture (Infrastructure Only)

> ⚠️ **Implementation Status**: This is infrastructure code only with no user-facing functionality. The bounded context exists (`contexts/device-synchronization/`) but is not connected to any CLI commands or user features. This documentation describes the planned architecture for future implementation.

## Core Concepts

### Local-First Architecture

Scopes follows a local-first approach where:
- Each device maintains a complete local database
- All operations work offline without network access
- Synchronization happens opportunistically when connected
- User experience is never blocked by sync operations

### Synchronization Model

The system uses an event-based synchronization model:

```kotlin
interface SyncEvent {
    val deviceId: String
    val timestamp: Instant
    val vectorClock: VectorClock
    val payload: DomainEvent
}
```

## Architecture Components

### 1. Device Registry

Each device is uniquely identified and tracked:

```kotlin
data class DeviceInfo(
    val deviceId: ULID,
    val deviceName: String,
    val platform: Platform,
    val lastSyncTime: Instant,
    val syncStatus: SyncStatus
)

enum class SyncStatus {
    SYNCHRONIZED,
    PENDING_SYNC,
    SYNCING,
    CONFLICT,
    OFFLINE
}
```

### 2. Sync Engine

The synchronization engine manages the sync lifecycle:

```kotlin
class SyncEngine {
    suspend fun syncWithPeer(peerId: DeviceId): SyncResult {
        // 1. Exchange vector clocks
        // 2. Determine missing events
        // 3. Transfer events
        // 4. Apply events locally
        // 5. Resolve conflicts if any
    }
}
```

### 3. Conflict Resolution

Conflicts are resolved using a combination of strategies:

#### Operational Transformation (OT)
For text and structured data changes:
```kotlin
class OperationalTransform {
    fun transform(
        localOp: Operation,
        remoteOp: Operation,
        context: TransformContext
    ): Pair<Operation, Operation>
}
```

#### Last-Write-Wins (LWW)
For simple value conflicts:
```kotlin
class LastWriteWinsResolver {
    fun resolve(
        local: Change,
        remote: Change
    ): Change = if (local.timestamp > remote.timestamp) local else remote
}
```

#### Custom Resolution Strategies
Domain-specific resolution for complex scenarios:
```kotlin
interface ConflictResolver<T> {
    fun resolve(local: T, remote: T, context: ConflictContext): T
}
```

### 4. Vector Clocks

Vector clocks ensure causal ordering of events:

```kotlin
class VectorClock(
    private val clocks: MutableMap<DeviceId, Long> = mutableMapOf()
) {
    fun increment(deviceId: DeviceId) {
        clocks[deviceId] = (clocks[deviceId] ?: 0) + 1
    }

    fun merge(other: VectorClock) {
        other.clocks.forEach { (device, time) ->
            clocks[device] = maxOf(clocks[device] ?: 0, time)
        }
    }

    fun happensBefore(other: VectorClock): Boolean {
        return clocks.all { (device, time) ->
            time <= (other.clocks[device] ?: 0)
        }
    }
}
```

## Synchronization Protocol

### 1. Peer Discovery

Devices discover each other through:
- **Local Network**: mDNS/Bonjour for LAN discovery
- **Cloud Relay**: Optional cloud service for internet sync
- **Direct Connection**: Bluetooth or USB for offline sync

### 2. Sync Negotiation

```
Device A                    Device B
    |                           |
    |------ SYNC_REQUEST ------>|
    |<----- SYNC_ACCEPT ---------|
    |                           |
    |------ VECTOR_CLOCK ------>|
    |<----- VECTOR_CLOCK --------|
    |                           |
    |------ MISSING_EVENTS ----->|
    |<----- MISSING_EVENTS ------|
    |                           |
    |------ EVENT_BATCH -------->|
    |<----- EVENT_BATCH ---------|
    |                           |
    |------ SYNC_COMPLETE ------>|
    |<----- SYNC_COMPLETE -------|
```

### 3. Delta Synchronization

Only changes since last sync are transferred:

```kotlin
class DeltaSync {
    suspend fun computeDelta(
        lastSyncTime: Instant,
        peerVectorClock: VectorClock
    ): List<SyncEvent> {
        return eventStore.getEventsSince(lastSyncTime)
            .filter { event ->
                // Event not yet seen by peer if it doesn't happen before peer's clock
                !event.vectorClock.happensBefore(peerVectorClock)
            }
    }
}
```

## Data Consistency Guarantees

### Eventual Consistency
- All devices eventually converge to the same state
- No central authority required
- Works with any network topology

### Causal Consistency
- Events maintain causal relationships
- Parent-child relationships preserved
- Dependency order respected

### Conflict-Free Replicated Data Types (CRDTs)
For specific data structures:

```kotlin
class GrowOnlySet<T> : CRDT<Set<T>> {
    private val elements = mutableSetOf<T>()

    fun add(element: T) {
        elements.add(element)
    }

    override fun merge(other: Set<T>): Set<T> {
        return elements + other
    }
}
```

## Offline Support

### Local Queue

Operations performed offline are queued:

```kotlin
class OfflineQueue {
    private val pendingOperations = mutableListOf<Operation>()

    suspend fun enqueue(operation: Operation) {
        pendingOperations.add(operation)
        persistQueue()
    }

    suspend fun flush(): List<Operation> {
        val ops = pendingOperations.toList()
        pendingOperations.clear()
        return ops
    }
}
```

### Automatic Retry

Failed sync operations are retried with exponential backoff:

```kotlin
class SyncRetryPolicy {
    suspend fun executeWithRetry(
        operation: suspend () -> SyncResult
    ): SyncResult {
        var delay = 1000L // Start with 1 second
        repeat(MAX_RETRIES) {
            try {
                return operation()
            } catch (e: SyncException) {
                delay(delay)
                delay *= 2 // Exponential backoff
            }
        }
        throw SyncFailedException()
    }
}
```

## Security Considerations

### End-to-End Encryption
- All sync data encrypted before transmission
- Device-specific keys for authentication
- Perfect forward secrecy for session keys

### Authentication
```kotlin
class DeviceAuth {
    suspend fun authenticate(
        deviceId: DeviceId,
        challenge: ByteArray
    ): AuthToken {
        val signature = signChallenge(challenge, devicePrivateKey)
        return AuthToken(deviceId, signature)
    }
}
```

### Data Integrity
- HMAC for message authentication
- Merkle trees for efficient verification
- Checksums for corruption detection

## Performance Optimization

### Compression
- Event batches compressed before transmission
- Binary encoding for efficient serialization
- Delta compression for similar events

### Bandwidth Management
```kotlin
class BandwidthManager {
    suspend fun throttleSync(
        available: Bandwidth,
        events: List<SyncEvent>
    ): Flow<SyncEvent> {
        return events.asFlow()
            .buffer(calculateBufferSize(available))
            .onEach { delay(calculateDelay(available)) }
    }
}
```

### Selective Sync
Users can configure what to sync:
- Specific scopes or contexts
- Time ranges
- Priority levels

## Implementation Status

### Infrastructure Code (Exists but Unused)
- ✅ Bounded context structure (`contexts/device-synchronization/`)
- ✅ Basic domain models and interfaces
- ✅ Placeholder implementations

### Not Implemented (No User Features)
- ❌ CLI commands for device management
- ❌ Actual synchronization logic
- ❌ Network protocols
- ❌ Conflict resolution
- ❌ Device discovery
- ❌ Data transfer mechanisms
- ❌ Integration with scope management

### Future Work Required
- ⏳ Connect infrastructure to application layer
- ⏳ Implement CLI commands (`scopes device` subcommands)
- ⏳ Create actual sync protocols
- ⏳ Build peer-to-peer networking
- ⏳ Integrate with event sourcing
- ⏳ Add user configuration options

## Testing Strategy

### Unit Tests
- Vector clock operations
- Conflict resolution algorithms
- Delta computation

### Integration Tests
- End-to-end sync scenarios
- Network failure handling
- Conflict resolution workflows

### Simulation Tests
```kotlin
class SyncSimulation {
    @Test
    fun `simulate concurrent edits on multiple devices`() {
        val devices = createDevices(count = 5)
        val operations = generateRandomOperations(count = 1000)

        // Distribute operations across devices
        // Simulate network partitions
        // Verify eventual consistency
    }
}
```

## Configuration

### Sync Settings
```yaml
sync:
  enabled: true
  mode: automatic  # automatic | manual | wifi-only
  interval: 300    # seconds
  max-batch-size: 1000
  compression: true
  encryption: true
```

### Device Pairing (Planned - Not Yet Implemented)

> **Note**: These CLI commands are planned but not yet implemented. Device synchronization currently exists only as infrastructure code without user-facing functionality.

```bash
# Pair with another device (planned)
scopes device pair <device-code>

# List paired devices (planned)
scopes device list

# Remove device (planned)
scopes device remove <device-id>
```

## Future Enhancements

### Short-term
- WebRTC for direct peer connections
- Improved conflict UI
- Sync progress visualization

### Long-term
- Distributed consensus protocols
- Blockchain for audit trail
- AI-powered conflict resolution

## Related Documentation

- [Event Sourcing Architecture](./event-sourcing-architecture.md) - Foundation for sync
- [Local-First Architecture](./adr/0001-local-first-architecture.md) - Design principles
- <!-- [Security Model](./security-model.md) - Encryption and authentication (Coming soon) -->
- [CLI Reference](../reference/cli-quick-reference.md) - Device commands
