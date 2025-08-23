# UseCase Architecture Style Guide

## Overview

This document defines the UseCase pattern implementation for the Scopes project, following Domain-Driven Design (DDD) and Clean Architecture principles. The pattern ensures consistent, testable, and maintainable application layer code.

## Core Pattern Components

### 1. UseCase Interface

```kotlin
typealias UseCaseResult<T> = Either<ApplicationError, T>

fun interface UseCase<I, T> {
    suspend operator fun invoke(input: I): UseCaseResult<T>
}
```

- **Functional interface**: Single abstract method for simplicity
- **Generic types**: `I` for input, `T` for success result type
- **Unified error handling**: All use cases return `UseCaseResult<T>` with standardized `ApplicationError`
- **Type-safe error handling**: Compile-time guarantee of consistent error handling
- **Operator function**: Enables direct invocation `useCase(input)`
- **Suspend function**: Supports coroutines for async operations

### 2. Command/Query Separation (CQRS)

#### Commands (Write Operations)
```kotlin
interface Command  // Marker interface for state-changing operations

data class CreateScope(
    val title: String,
    val description: String? = null,
    val parentId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
) : Command
```

#### Queries (Read Operations)
```kotlin
interface Query    // Marker interface for read-only operations

data class GetScopeById(
    val id: String
) : Query
```

### 3. Handlers

```kotlin
class CreateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val applicationScopeValidationService: ApplicationScopeValidationService
) : UseCase<CreateScope, CreateScopeResult> {

    override suspend operator fun invoke(input: CreateScope): UseCaseResult<CreateScopeResult> = either {
        // Orchestration logic here
    }
}
```

#### Handler Responsibilities:
- **Orchestrate** domain services and repositories
- **Handle** transactions (one handler = one transaction boundary)
- **Map** between domain entities and DTOs
- **Validate** cross-aggregate business rules
- **NOT contain** business logic (delegate to domain)

### 4. Result DTOs

```kotlin
data class CreateScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Instant,
    val metadata: Map<String, String>
)
```

#### DTO Requirements:
- **No domain entities**: Only primitive types and value objects
- **Immutable**: Data classes with `val` properties
- **Serializable**: Support JSON/API responses

### 5. Mappers

Mappers are responsible for converting between domain entities and application DTOs, ensuring that domain types never leak outside the application layer.

```kotlin
object ScopeMapper {
    
    /**
     * Map Scope entity to CreateScopeResult DTO.
     */
    fun toCreateScopeResult(scope: Scope): CreateScopeResult = 
        CreateScopeResult(
            id = scope.id.toString(),
            title = scope.title.value,
            description = scope.description?.value,
            parentId = scope.parentId?.toString(),
            createdAt = scope.createdAt,
            metadata = scope.metadata.toMap()  // Defensive copy for immutability
        )
}
```

#### Mapper Rules and Constraints:

1. **Location**: Mappers MUST live only in `application/mapper/` package
2. **Purity**: No business logic inside mappers - only data transformation
3. **Boundary Protection**: Domain types never leak outside mappers
4. **Immutability**: Always create defensive copies for mutable collections
5. **One-Way Flow**: Domain Entity → Mapper → DTO → UseCaseResult

#### Integration with UseCase Pattern:

Mappers integrate seamlessly with the UseCase pattern at the final step of handlers:

**Before (Inline Mapping - Avoid)**:
```kotlin
class CreateScopeHandler(...) : UseCase<CreateScope, CreateScopeError, CreateScopeResult> {
    
    override suspend operator fun invoke(input: CreateScope): Either<CreateScopeError, CreateScopeResult> {
        // ... domain logic ...
        
        // ❌ Inline mapping clutters handler logic
        return CreateScopeResult(
            id = savedScope.id.toString(),
            title = savedScope.title.value,
            description = savedScope.description?.value,
            parentId = savedScope.parentId?.toString(),
            createdAt = savedScope.createdAt,
            metadata = savedScope.metadata.toMap()
        ).right()
    }
}
```

**After (Dedicated Mapper - Preferred)**:
```kotlin
class CreateScopeHandler(...) : UseCase<CreateScope, CreateScopeResult> {
    
    override suspend operator fun invoke(input: CreateScope): UseCaseResult<CreateScopeResult> {
        // ... domain logic ...
        
        // ✅ Clean separation using dedicated mapper
        return ScopeMapper.toCreateScopeResult(savedScope).right()
    }
}
```

#### Benefits:

- **Separation of Concerns**: Handlers focus on orchestration, mappers handle transformation
- **Reusability**: Same mapper can be used across multiple handlers
- **Testability**: Mapping logic can be tested independently
- **Maintainability**: Changes to DTO structure are centralized in mappers
- **Architecture Compliance**: Enforces Clean Architecture boundary rules

## Directory Structure

```
application/
├── usecase/
│   ├── UseCase.kt                    # Base interface
│   ├── command/
│   │   ├── Command.kt                # Marker interface
│   │   └── CreateScope.kt            # Command DTOs
│   ├── query/
│   │   ├── Query.kt                  # Marker interface
│   │   └── GetScopeById.kt           # Query DTOs
│   └── handler/
│       ├── CreateScopeHandler.kt     # Command handlers
│       └── GetScopeByIdHandler.kt    # Query handlers
├── dto/
│   ├── CreateScopeResult.kt          # Result DTOs
│   └── ScopeDto.kt                   # Query result DTOs
└── mapper/
    └── ScopeMapper.kt                # Domain ↔ DTO mapping
```

## Implementation Checklist

### ✅ UseCase Definition
- [ ] Handler implements `UseCase<Input, Output>`
- [ ] Input implements `Command` or `Query` marker
- [ ] Output is a DTO (no domain entities)
- [ ] Handler name ends with "Handler"

### ✅ Mapper Requirements
- [ ] Mappers located only in `application/mapper/` package
- [ ] No business logic inside mappers (pure transformation)
- [ ] Domain entities converted to DTOs before leaving application layer
- [ ] Defensive copies created for mutable collections
- [ ] Handler uses dedicated mapper instead of inline mapping

### ✅ Dependency Management  
- [ ] Handler depends only on domain interfaces
- [ ] No infrastructure dependencies in application layer
- [ ] Repository injected via constructor
- [ ] Domain services injected when needed

### ✅ Error Handling
- [ ] Uses `UseCaseResult<T>` for error handling
- [ ] Domain errors mapped to application errors
- [ ] Repository errors wrapped appropriately
- [ ] Validation errors accumulated for better UX

### ✅ Transaction Management
- [ ] One handler = one transaction boundary
- [ ] Transaction scope clearly defined
- [ ] Rollback strategy documented

### ✅ Testing
- [ ] Unit tests for handler logic
- [ ] Mock all dependencies
- [ ] Test error paths
- [ ] Test successful scenarios

## Architecture Rules (Enforced by Konsist)

1. **Dependency Direction**:
   - Domain depends on nothing
   - Application depends only on domain
   - Infrastructure implements domain/application ports
   - Presentation depends only on application

2. **Naming Conventions**:
   - Handlers end with "Handler"
   - Commands implement `Command`
   - Queries implement `Query`
   - DTOs don't expose domain entities

3. **Layer Isolation**:
   - No infrastructure imports in application
   - No domain entities in DTOs
   - Mappers handle entity ↔ DTO conversion

4. **Mapper Rules**:
   - Mappers reside only in `application/mapper/`
   - No business logic in mappers (pure data transformation)
   - Domain entities never escape mappers
   - Use defensive copying for mutable collections

## Example Usage

### CLI Integration

```kotlin
class CreateScopeCommand(
    private val createScopeHandler: CreateScopeHandler,
) : CliktCommand(name = "create-scope") {
    
    private val name by option("--name").required()
    
    override fun run() = runBlocking {
        val command = CreateScope(title = name)
        
        createScopeHandler(command).fold(
            ifLeft = { error -> /* Handle error */ },
            ifRight = { result -> /* Display success */ }
        )
    }
}
```

### Dependency Injection

```kotlin
val applicationModule = module {
    single { CreateScopeHandler(get(), get()) }
}
```

### Complete Data Flow Example

```kotlin
// Step 1: Domain Entity (with rich domain types)
val scope = Scope(
    id = ScopeId.generate(),
    title = ScopeTitle.from("Project Alpha"),
    description = ScopeDescription.from("Strategic project"),
    parentId = ScopeId.from("parent-123"),
    createdAt = Clock.System.now(),
    metadata = mutableMapOf("priority" to "high")
)

// Step 2: Mapper transforms domain entity to DTO
val dto = ScopeMapper.toCreateScopeResult(scope)
// Result: CreateScopeResult(
//   id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
//   title = "Project Alpha", 
//   description = "Strategic project",
//   parentId = "parent-123",
//   createdAt = 2024-01-15T10:30:00Z,
//   metadata = mapOf("priority" to "high")
// )

// Step 3: Handler returns DTO wrapped in Either
return dto.right()
```

This flow ensures:
- **Domain types** (ScopeId, ScopeTitle) stay within domain/application boundary
- **Primitive types** (String, Instant) are exposed to presentation layer
- **Clean separation** between domain complexity and API simplicity

## Benefits

1. **Consistency**: All use cases follow the same pattern
2. **Testability**: Easy to unit test with mocked dependencies
3. **Maintainability**: Clear separation of concerns
4. **Scalability**: Easy to add new use cases
5. **Documentation**: Self-documenting through types
6. **Architecture Compliance**: Enforced by Konsist tests

## Migration from Legacy Pattern

The existing `CreateScopeUseCase` class can coexist with the new pattern during transition:

```kotlin
// Legacy (to be deprecated)
class CreateScopeUseCase(...)

// New pattern
class CreateScopeHandler(...) : UseCase<CreateScope, CreateScopeResult>
```

Both are registered in DI container until migration is complete.

## Transaction Example (Future Implementation)

```kotlin
@Transactional
class CreateScopeHandler(...) : UseCase<CreateScope, CreateScopeResult> {
    override suspend operator fun invoke(input: CreateScope): UseCaseResult<CreateScopeResult> = either {
        // All operations within this handler are in one transaction
        // Automatic rollback on any error
    }
}
```

## Example Vertical Slice (E2E)

The following example demonstrates the complete DDD UseCase pattern implementation from CLI to domain and back:

### Running the Sample

Execute the sample CLI command to create a scope:

```bash
# Basic scope creation
./gradlew run --args="create --name Hello"

# Expected output:
✅ Created scope: 01K292J7NFNTDC1T8XQKEV0CG5
   Title: Hello
   Created at: 2025-08-10T04:07:05.134639782Z
```

```bash
# Advanced scope creation with description
./gradlew run --args="create --name 'My First Project' --description 'A sample project for the demo'"

# Expected output:
✅ Created scope: 01K292JDV26D6D1SGMK1T60Q2W
   Title: My First Project
   Description: A sample project for the demo
   Created at: 2025-08-10T04:07:11.456063133Z
```

### What the Demo Demonstrates

This E2E slice showcases the complete vertical flow through all architecture layers:

1. **CLI Layer** (`apps:scopes`):
   - `CreateScopeCommand` captures user input
   - Validates required parameters
   - Maps CLI arguments to domain command

2. **Application Layer** (`application`):
   - `CreateScopeHandler` orchestrates the use case
   - `CreateScope` command represents user intent
   - `CreateScopeResult` DTO provides clean output
   - `ScopeMapper` handles entity-to-DTO transformation

3. **Domain Layer** (`domain`):
   - `Scope` entity enforces business rules
   - `ScopeTitle` value object validates title constraints
   - `ScopeId` generates ULID identifiers

4. **Infrastructure Layer** (`infrastructure`):
   - `InMemoryScopeRepository` persists data
   - `NoopTransactionManager` handles transaction boundaries

### Architecture Benefits Demonstrated

- **Clean Separation**: Each layer has clear responsibilities
- **Dependency Inversion**: High-level layers don't depend on low-level details
- **Testability**: Each component can be tested in isolation
- **Type Safety**: Strong typing prevents runtime errors
- **Error Handling**: Graceful error propagation through `UseCaseResult<T>`
- **Local-First**: Works without external dependencies using in-memory storage

### ULID Identifier System

The demo showcases ULID (Universally Unique Lexicographically Sortable Identifier) usage:
- **Format**: 26 characters (e.g., `01K292J7NFNTDC1T8XQKEV0CG5`)
- **Sortable**: Chronologically ordered by creation time  
- **Distributed**: No central coordination required
- **URL-Safe**: Can be used in REST APIs and file paths

This pattern provides a solid foundation for scaling the application while maintaining architectural integrity and code quality.
