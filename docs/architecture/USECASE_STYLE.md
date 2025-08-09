# UseCase Architecture Style Guide

## Overview

This document defines the UseCase pattern implementation for the Scopes project, following Domain-Driven Design (DDD) and Clean Architecture principles. The pattern ensures consistent, testable, and maintainable application layer code.

## Core Pattern Components

### 1. UseCase Interface

```kotlin
fun interface UseCase<I, O> {
    suspend operator fun invoke(input: I): O
}
```

- **Functional interface**: Single abstract method for simplicity
- **Generic types**: `I` for input, `O` for output
- **Operator function**: Enables direct invocation `useCase(input)`
- **Suspend function**: Supports coroutines for async operations

### 2. Command/Query Separation (CQRS)

#### Commands (Write Operations)
```kotlin
interface Command  // Marker interface for state-changing operations

data class CreateScope(
    val title: String,
    val description: String? = null,
    val parentId: ScopeId? = null,
    val metadata: Map<String, String> = emptyMap(),
) : Command
```

#### Queries (Read Operations)
```kotlin
interface Query    // Marker interface for read-only operations

data class GetScopeById(
    val id: ScopeId
) : Query
```

### 3. Handlers

```kotlin
class CreateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val applicationScopeValidationService: ApplicationScopeValidationService
) : UseCase<CreateScope, Either<ApplicationError, CreateScopeResult>> {

    override suspend operator fun invoke(input: CreateScope): Either<ApplicationError, CreateScopeResult> = either {
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
    val id: ScopeId,
    val title: String,
    val description: String?,
    val parentId: ScopeId?,
    val createdAt: Instant,
    val metadata: Map<String, String>
)
```

#### DTO Requirements:
- **No domain entities**: Only primitive types and value objects
- **Immutable**: Data classes with `val` properties
- **Serializable**: Support JSON/API responses

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

### ✅ Dependency Management  
- [ ] Handler depends only on domain interfaces
- [ ] No infrastructure dependencies in application layer
- [ ] Repository injected via constructor
- [ ] Domain services injected when needed

### ✅ Error Handling
- [ ] Uses `Either<Error, Result>` for error handling
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
class CreateScopeHandler(...) : UseCase<CreateScope, Either<ApplicationError, CreateScopeResult>>
```

Both are registered in DI container until migration is complete.

## Transaction Example (Future Implementation)

```kotlin
@Transactional
class CreateScopeHandler(...) : UseCase<CreateScope, Either<ApplicationError, CreateScopeResult>> {
    override suspend operator fun invoke(input: CreateScope): Either<ApplicationError, CreateScopeResult> = either {
        // All operations within this handler are in one transaction
        // Automatic rollback on any error
    }
}
```

This pattern provides a solid foundation for scaling the application while maintaining architectural integrity and code quality.