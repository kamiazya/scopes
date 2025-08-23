# Domain-Driven Design Implementation

## Overview

Scopes implements **Domain-Driven Design (DDD)** with a functional programming approach using Kotlin and Arrow.

## Bounded Contexts

The system is organized into bounded contexts, each representing a distinct business capability:

```mermaid
graph TB
    subgraph "Scope Management Context"
        subgraph "Domain"
            AGG[Scope Aggregate]
            ENT[Entities]
            VO[Value Objects]
            EVT[Domain Events]
            SVC[Domain Services]
        end
        
        subgraph "Application"
            CMD[Commands]
            QRY[Queries]
            HANDLER[Handlers]
            APPSVC[App Services]
        end
        
        subgraph "Infrastructure"
            REPO[Repositories]
            ALIAS[Alias Generation]
            TRANS[Transactions]
        end
    end
    
    subgraph "Future Contexts"
        UP[User Preferences]
        WM[Workspace Management]
        AC[AI Collaboration]
    end
    
    AGG --> ENT
    AGG --> VO
    AGG --> EVT
    ENT --> VO
    SVC --> AGG
    
    HANDLER --> AGG
    HANDLER --> SVC
    APPSVC --> HANDLER
    
    REPO --> AGG
    ALIAS --> SVC
    
    classDef domain fill:#e8f5e9
    classDef application fill:#fff3e0
    classDef infrastructure fill:#fce4ec
    classDef future fill:#f5f5f5,stroke-dasharray: 5 5
    
    class AGG,ENT,VO,EVT,SVC domain
    class CMD,QRY,HANDLER,APPSVC application
    class REPO,ALIAS,TRANS infrastructure
    class UP,WM,AC future
```

### Current Bounded Contexts

#### Scope Management Context
- **Responsibility**: Core scope operations, hierarchy, aspects, aliases
- **Location**: `contexts/scope-management/`
- **Status**: Implemented
- **Key Aggregates**: ScopeAggregate
- **Key Services**: ScopeHierarchyService, AliasGenerationService
- **Ubiquitous Language**:
  - Scope (スコープ): Unified recursive work unit
  - Scope ID: Unique identifier (ULID)
  - Scope Title: Identifying name
  - Parent ID: Hierarchical relationship
  - Aspects: Key-value metadata
  - Context View: Filtered view of scopes

### Future Bounded Contexts (Planned)

#### User Preferences Context
- **Responsibility**: User-specific settings and preferences management
- **Status**: Not yet implemented
- **Relationship**: Customer-Supplier to other contexts
- **Ubiquitous Language**:
  - Preferences: User settings
  - Theme: UI color scheme
  - Editor Config: Editor settings
  - Default Values: Initial values for new items

#### Workspace Management Context
- **Responsibility**: File system integration and focus management
- **Status**: Not yet implemented
- **Relationship**: Partnership with Scope Management
- **Ubiquitous Language**:
  - Workspace: Work directory associated with scope
  - Focus: Current working scope attention
  - Context Switch: Moving between workspaces

#### AI Collaboration Context
- **Responsibility**: AI assistant integration and asynchronous collaboration
- **Status**: Not yet implemented
- **Integration**: MCP protocol, comment-based interaction
- **Ubiquitous Language**:
  - AI Comment: Request to AI
  - Conversation Thread: Series of AI interactions
  - Handoff: Work transfer between AI assistants
  - Co-authorship: AI collaboration records

### Context Mapping

```mermaid
graph LR
    SM[Scope Management<br/>Context]
    UP[User Preferences<br/>Context]
    WM[Workspace Management<br/>Context]
    AC[AI Collaboration<br/>Context]
    
    %% Relationships
    UP -->|Customer-Supplier| SM
    UP -->|Customer-Supplier| WM
    UP -->|Customer-Supplier| AC
    
    SM <-->|Partnership| WM
    WM -->|Open Host Service| AC
    
    %% Context types
    SM:::core
    UP:::supporting
    WM:::generic
    AC:::generic
    
    classDef core fill:#ff9999,stroke:#333,stroke-width:3px
    classDef supporting fill:#99ccff,stroke:#333,stroke-width:2px
    classDef generic fill:#99ff99,stroke:#333,stroke-width:1px
```

### Domain Boundaries

- **Core Domain**: Scope management with recursive hierarchy
- **Supporting Domains**: User preferences, aspect system, alias management
- **Generic Domains**: Workspace management, AI collaboration
- **Anti-Corruption Layer**: DTOs and facades protect domain from external changes

## Strategic Design

### Ubiquitous Language

Key terms used consistently throughout the codebase:

- **Scope**: Unified entity representing any work unit (project, task, etc.)
- **Aspect**: Key-value metadata for classification and querying
- **Hierarchy**: Parent-child relationships between scopes
- **Focus**: Currently selected scope for context
- **Workspace**: Directory-based project context

### Core Domain

The core domain is **recursive scope management** - the innovative concept that all work units are unified scopes with consistent operations.

### Supporting Domains

- **Aspect Management**: Flexible metadata system (currently embedded)
- **Comment System**: AI and human collaboration
- **Attachment Management**: File and resource linking

### Generic Domains

- **Authentication**: User identity (future)
- **Synchronization**: External tool integration
- **Notification**: Event broadcasting

## Tactical Design

### Aggregates

#### ScopeAggregate

The main aggregate root implementing event sourcing pattern:

```kotlin
data class ScopeAggregate(
    override val id: AggregateId,
    override val version: AggregateVersion,
    val createdAt: Instant,
    val updatedAt: Instant,
    val scope: Scope?,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
) : AggregateRoot<ScopeAggregate>() {
    
    companion object {
        // Factory method for creating new aggregates
        fun create(
            title: ScopeTitle,
            description: ScopeDescription? = null,
            parentId: ScopeId? = null,
            aspects: Aspects = Aspects.empty(),
            clock: Clock = Clock.System
        ): Either<ScopesError, Pair<ScopeAggregate, DomainEvent>>
    }
    
    // Command methods that produce events
    fun updateTitle(
        newTitle: ScopeTitle,
        clock: Clock = Clock.System
    ): Either<ScopesError, Pair<ScopeAggregate, DomainEvent>> = either {
        ensureNotDeleted().bind()
        ensureNotArchived().bind()
        val currentScope = ensureScopeExists().bind()
        
        val event = ScopeTitleUpdated(
            eventId = EventId.generate(),
            aggregateId = id,
            scopeId = currentScope.id,
            oldTitle = currentScope.title,
            newTitle = newTitle,
            occurredAt = clock.now()
        )
        
        val updated = copy(
            scope = currentScope.copy(title = newTitle),
            version = version.increment(),
            updatedAt = clock.now()
        )
        
        updated to event
    }
    
    // Apply events to reconstruct state (Event Sourcing)
    fun applyEvent(event: DomainEvent): ScopeAggregate = when (event) {
        is ScopeCreated -> copy(
            scope = event.scope,
            version = version.increment(),
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt
        )
        is ScopeTitleUpdated -> copy(
            scope = scope?.copy(title = event.newTitle),
            version = version.increment(),
            updatedAt = event.occurredAt
        )
        is ScopeDeleted -> copy(
            isDeleted = true,
            version = version.increment(),
            updatedAt = event.occurredAt
        )
        // ... other events
    }
}

### Entities

#### Scope Entity

The core entity with identity and lifecycle:

```kotlin
data class Scope(
    val id: ScopeId,                    // Identity
    val title: ScopeTitle,               // Required
    val description: ScopeDescription?,  // Optional
    val parentId: ScopeId?,              // Hierarchy
    val createdAt: Instant,
    val updatedAt: Instant,
    val aspects: Map<AspectKey, NonEmptyList<AspectValue>>
) {
    // Business logic methods
    fun addChild(child: Scope): Either<ScopesError, Scope>
    fun updateAspect(key: AspectKey, value: AspectValue): Either<ScopesError, Scope>
    fun canBeDeleted(): Boolean
}
```

### Value Objects

Immutable objects without identity:

```kotlin
// Strong typing with validation
@JvmInline
value class ScopeTitle private constructor(val value: String) {
    companion object {
        fun create(value: String?): Either<ScopesError, ScopeTitle?> = either {
            when {
                value.isNullOrBlank() -> null
                value.length > 200 -> raise(TitleTooLongError(value.length))
                else -> ScopeTitle(value.trim())
            }
        }
    }
}

// Unique identifier
@JvmInline
value class ScopeId private constructor(val value: String) {
    companion object {
        fun generate(): ScopeId = ScopeId(ULID.randomULID())
        fun parse(value: String): Either<ScopesError, ScopeId> = 
            Either.catch { ScopeId(ULID.parseULID(value)) }
                .mapLeft { InvalidScopeIdError(value) }
    }
}
```

### Domain Events

Events capture important state changes:

```kotlin
sealed interface DomainEvent {
    val aggregateId: AggregateId
    val occurredAt: Instant
}

data class ScopeCreated(
    override val aggregateId: AggregateId,
    val scope: Scope,
    override val occurredAt: Instant
) : DomainEvent

data class ScopeUpdated(
    override val aggregateId: AggregateId,
    val scope: Scope,
    val changes: Set<String>,  // What changed
    override val occurredAt: Instant
) : DomainEvent

data class ScopeDeleted(
    override val aggregateId: AggregateId,
    val scopeId: ScopeId,
    override val occurredAt: Instant
) : DomainEvent
```

### Domain Services

For operations that don't belong to a single entity:

```kotlin
class HierarchyValidationService {
    fun validateHierarchy(
        scope: Scope,
        allScopes: List<Scope>
    ): Either<ScopesError, Unit> = either {
        ensureNoCircularReference(scope, allScopes).bind()
        ensureDepthLimit(scope, allScopes).bind()
        ensureParentExists(scope, allScopes).bind()
    }
}
```

### Repository Interfaces

Abstract persistence in domain layer:

```kotlin
interface ScopeRepository {
    fun save(aggregate: ScopeAggregate): Either<ScopesError, Unit>
    fun findById(id: ScopeId): Either<ScopesError, ScopeAggregate?>
    fun findByWorkspace(workspaceId: WorkspaceId): Either<ScopesError, List<ScopeAggregate>>
    fun delete(id: ScopeId): Either<ScopesError, Unit>
}
```

## Functional DDD with Arrow

### Immutability
All domain objects are immutable data classes:

```kotlin
// ❌ Wrong: Mutable state
class Scope {
    var title: String = ""
    fun setTitle(value: String) { title = value }
}

// ✅ Correct: Immutable with copy
data class Scope(val title: ScopeTitle) {
    fun updateTitle(newTitle: ScopeTitle): Scope = copy(title = newTitle)
}
```

### Pure Functions
Business logic as pure functions without side effects:

```kotlin
// Pure function - deterministic and no side effects
fun calculateCompletionPercentage(
    scope: Scope,
    children: List<Scope>
): Int {
    val completed = children.count { it.isCompleted() }
    return if (children.isEmpty()) 0 
           else (completed * 100) / children.size
}
```

### Error Handling with Either
Explicit error handling without exceptions:

```kotlin
fun createScope(
    title: String,
    parentId: String?
): Either<ScopesError, Scope> = either {
    val validTitle = ScopeTitle.create(title).bind()
    val validParentId = parentId?.let { ScopeId.parse(it).bind() }
    
    Scope.create(
        title = validTitle,
        parentId = validParentId
    ).bind()
}
```

## Testing Strategy

### Property-Based Testing
Test invariants with random data:

```kotlin
class ScopeTitleTest : FunSpec({
    test("title should be trimmed") {
        checkAll(Arb.string()) { input ->
            ScopeTitle.create(input).fold(
                { true }, // Error case is valid
                { it?.value == input.trim() }
            )
        }
    }
    
    test("title length should not exceed 200") {
        checkAll(Arb.string(201..500)) { input ->
            ScopeTitle.create(input).isLeft()
        }
    }
})
```

### Domain Event Testing
Verify correct events are produced:

```kotlin
test("updating scope should produce ScopeUpdated event") {
    val aggregate = createTestAggregate()
    val command = UpdateScope(aggregate.id, newTitle = "Updated")
    
    val result = aggregate.execute(command)
    
    result.shouldBeRight()
    result.value.events.last().shouldBeInstanceOf<ScopeUpdated>()
}
```

## Benefits of DDD in Scopes

1. **Business Alignment**: Code reflects business concepts directly
2. **Maintainability**: Clear boundaries and responsibilities
3. **Flexibility**: Easy to evolve with changing requirements
4. **Testability**: Business logic isolated from infrastructure
5. **Team Communication**: Ubiquitous language reduces misunderstandings

## Layer Responsibilities

### Application Layer (within Bounded Context)
Handles business logic within a single bounded context:

```kotlin
// contexts/scope-management/application/usecase/CreateScopeUseCase.kt
class CreateScopeUseCase(
    private val scopeRepository: ScopeRepository,
    private val eventPublisher: DomainEventPublisher
) {
    fun execute(command: CreateScopeCommand): Either<ScopesError, Scope> = either {
        // Single context business logic
        val scope = Scope.create(command.title, command.parentId).bind()
        scopeRepository.save(scope)
        eventPublisher.publish(ScopeCreatedEvent(scope))
        scope
    }
}
```

**Responsibilities**:
- Single context business logic
- Domain service coordination within context
- Transaction management
- Domain event publishing

### Interface Layer (Cross-Context Integration)
Coordinates multiple bounded contexts and adapts to external interfaces:

```kotlin
// interfaces/cli/adapters/ScopeCommandAdapter.kt
class ScopeCommandAdapter(
    private val scopeManagement: ScopeManagementFacade,
    private val workspaceManagement: WorkspaceManagementFacade?,
    private val aiCollaboration: AiCollaborationFacade?
) {
    fun createScope(title: String, parentId: String?): Either<ScopesError, ScopeDto> = either {
        // Coordinate multiple contexts
        val scope = scopeManagement.createScope(title, parentId).bind()
        workspaceManagement?.initializeWorkspace(scope.id)
        aiCollaboration?.notifyCreation(scope)
        scope
    }
}
```

**Responsibilities**:
- Cross-context coordination
- Protocol adaptation (CLI/API/MCP/SDK)
- Cross-cutting concerns (auth, logging)
- DTO conversion for external exposure

## Implementation Guidelines

1. Start with Event Storming to discover domain events
2. Model aggregates around consistency boundaries
3. Use value objects for type safety and validation
4. Keep aggregates small and focused
5. Use domain events for decoupling
6. Implement repositories as interfaces in domain
7. Test business invariants with property-based tests
8. Use Application layer for single-context orchestration
9. Use Interface layer for cross-context integration
