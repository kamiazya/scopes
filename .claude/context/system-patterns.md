---
created: 2025-08-25T14:33:19Z
last_updated: 2025-08-25T14:33:19Z
version: 1.0
author: Claude Code PM System
---

# System Patterns

## Architectural Patterns

### Clean Architecture
The project follows Uncle Bob's Clean Architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────┐
│                 Interfaces                  │ ← CLI, API, UI
├─────────────────────────────────────────────┤
│                Application                  │ ← Use Cases
├─────────────────────────────────────────────┤
│                  Domain                     │ ← Entities, VOs
├─────────────────────────────────────────────┤
│              Infrastructure                 │ ← Adapters
└─────────────────────────────────────────────┘
```

**Key Principles:**
- Dependencies point inward
- Domain layer has no external dependencies
- Use cases orchestrate domain logic
- Infrastructure implements domain interfaces

### Domain-Driven Design (DDD)

**Tactical Patterns:**
- **Entities**: Core business objects with identity (e.g., `ScopeEntity`)
- **Value Objects**: Immutable objects without identity (e.g., `ScopeId`, `Title`)
- **Aggregates**: Consistency boundaries around entities
- **Domain Events**: State changes as first-class concepts
- **Use Cases**: Application-specific business rules

**Strategic Patterns:**
- **Bounded Contexts**: Separate contexts for different business capabilities
- **Context Mapping**: Explicit relationships between contexts
- **Ubiquitous Language**: Consistent terminology throughout the codebase

### Hexagonal Architecture (Ports & Adapters)

```
         ┌─────────────┐
         │   CLI/API   │
         └──────┬──────┘
                │
    ┌───────────▼───────────┐
    │     Application       │
    │  ┌─────────────────┐  │
    │  │   Use Cases     │  │
    │  └────────┬────────┘  │
    │           │           │
    │  ┌────────▼────────┐  │
    │  │     Domain      │  │
    │  └─────────────────┘  │
    └───────────────────────┘
         ▲              ▲
         │              │
    ┌────┴────┐    ┌────┴────┐
    │Database │    │External │
    │Adapter  │    │Services │
    └─────────┘    └─────────┘
```

### Functional Programming Patterns

**Immutability First:**
- All domain objects are immutable
- State changes return new instances
- No side effects in domain logic

**Result Types:**
- Explicit error handling with `Result<T, E>`
- No exceptions in domain layer
- Railway-oriented programming

**Pure Functions:**
- Domain logic as pure functions
- Testable without mocks
- Composable operations

## Design Patterns

### Repository Pattern
- Abstract data access behind interfaces
- Domain defines repository contracts
- Infrastructure provides implementations

### Use Case Pattern
```kotlin
interface UseCase<in Request, out Response> {
    suspend fun execute(request: Request): Result<Response, DomainError>
}
```

### Factory Pattern
- Complex object creation encapsulated
- Domain factories for aggregate creation
- Ensures valid object construction

### Observer Pattern
- Domain events for state changes
- Event bus for cross-context communication
- Asynchronous event handling

## Data Flow Patterns

### Command Query Responsibility Segregation (CQRS)
- Separate read and write models
- Optimized queries for different use cases
- Event sourcing ready architecture

### Event-Driven Architecture
```
User Action → Command → Use Case → Domain Event → Event Handlers
                              ↓
                          State Change
```

### Dependency Injection
- Constructor injection preferred
- No service locators
- Compile-time dependency resolution

## Testing Patterns

### Test Pyramid
```
         ╱─────╲
        ╱  E2E  ╲
       ╱─────────╲
      ╱Integration╲
     ╱─────────────╲
    ╱     Unit      ╲
   ╱─────────────────╲
```

### Property-Based Testing
- Generate test cases for value objects
- Verify invariants hold
- Catch edge cases automatically

### Architecture Testing
- Konsist rules enforce architecture
- Automated validation of dependencies
- Prevent architecture degradation

## Error Handling Patterns

### Domain Errors as Types
```kotlin
sealed interface DomainError {
    data class ValidationError(val message: String) : DomainError
    data class NotFoundError(val id: String) : DomainError
    data class ConflictError(val reason: String) : DomainError
}
```

### Fail-Fast Validation
- Validate at system boundaries
- Rich error messages
- No defensive programming in domain

### Graceful Degradation
- Optional features fail silently
- Core functionality always available
- Clear error communication

## Concurrency Patterns

### Structured Concurrency
- Kotlin coroutines with proper scopes
- Cancellation propagation
- Resource cleanup guarantees

### Actor Model (Future)
- Scopes as actors
- Message passing for updates
- Concurrent operations without locks

## Security Patterns

### Zero Trust Architecture
- Validate all inputs
- Authenticate at boundaries
- Minimal privilege principle

### Local-First Security
- Encryption at rest
- No cloud dependency for core features
- User controls data sharing
