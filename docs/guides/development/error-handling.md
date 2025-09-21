# Error Handling Guidelines

This guide covers the comprehensive error handling architecture used in the Scopes project, including error hierarchies, translation patterns, and best practices.

## Table of Contents
- [Core Principles](#core-principles)
- [Error Hierarchy Architecture](#error-hierarchy-architecture)
- [Error Translation Patterns](#error-translation-patterns)
- [ValidationResult Pattern](#validationresult-pattern)
- [Error Mapping Boundaries](#error-mapping-boundaries)
- [Testing Error Handling](#testing-error-handling)
- [Best Practices](#best-practices)

## Core Principles

### Fail-Fast Philosophy

The system implements fail-fast error handling to prevent data corruption and ensure system reliability:

```kotlin
// ✅ Fail-fast implementation for unmapped errors
fun DomainScopeAliasError.toApplicationError(): ApplicationError = when (this) {
    is DomainScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound ->
        AppScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound(
            aliasName = this.aliasName,
            scopeId = this.scopeId.toString(),
        )
    
    // Fail fast for any unmapped DataInconsistencyError subtypes
    is DomainScopeAliasError.DataInconsistencyError ->
        error(
            "Unmapped DataInconsistencyError subtype: ${this::class.simpleName}. " +
                "Please add proper error mapping for this error type.",
        )
}
```

**Rationale for Fail-Fast Approach:**
- **Data Integrity**: Using "unknown" fallbacks masks real problems
- **Early Detection**: Issues are caught in development/testing phases  
- **No Silent Failures**: Better to fail loudly than corrupt data silently
- **Actionable Errors**: Error messages provide clear guidance for developers

### Functional Error Handling with Either

All error handling uses Arrow's Either type for explicit error propagation:

```kotlin
suspend fun processData(input: String): Either<ScopesError, Result> = either {
    val validated = validateInput(input).bind()
    val processed = processAsync(validated).bind()
    Result(processed)
}
```

## Error Hierarchy Architecture

### Service-Specific Error Hierarchies

Each service defines its own detailed error hierarchy with rich context information:

```mermaid
graph TB
    subgraph "Domain Error Types"
        DomainError[DomainError<br/>Base Class]

        subgraph "Validation Errors"
            TitleValidation[TitleValidationError]
            TitleValidation --> EmptyTitle[EmptyTitle]
            TitleValidation --> TooShort[TitleTooShort<br/>• minLength<br/>• actualLength<br/>• title]
            TitleValidation --> TooLong[TitleTooLong<br/>• maxLength<br/>• actualLength<br/>• title]
            TitleValidation --> InvalidChars[InvalidCharacters<br/>• invalidCharacters<br/>• position]
        end

        subgraph "Business Rule Errors"
            BusinessRule[ScopeBusinessRuleError]
            BusinessRule --> MaxDepth[MaxDepthExceeded<br/>• scopeId<br/>• currentDepth<br/>• maxDepth]
            BusinessRule --> MaxChildren[MaxChildrenExceeded<br/>• parentId<br/>• currentCount<br/>• maxChildren]
        end

        DomainError --> TitleValidation
        DomainError --> BusinessRule
    end

    classDef base fill:#f9f9f9,stroke:#666,stroke-width:2px
    classDef category fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
    classDef specific fill:#fff3e0,stroke:#ff9800,stroke-width:1px

    class DomainError base
    class TitleValidation,BusinessRule category
    class EmptyTitle,TooShort,TooLong,InvalidChars,MaxDepth,MaxChildren specific
```

**Key Principles:**
- Each error type carries contextual information needed for debugging and user feedback
- Errors are organized in sealed hierarchies for exhaustive pattern matching
- Specific error types include all data necessary to understand what went wrong
- Domain errors use domain types (e.g., `ScopeId`) to maintain type safety

### Layer-Specific Error Types

```mermaid
flowchart TD
    Domain["Domain Errors<br/>• DomainError<br/>• ScopeInputError<br/>• ScopeAliasError<br/>• ContextError<br/>• PersistenceError"]
    
    App["Application Errors<br/>• ApplicationError<br/>• ScopeInputError<br/>• ScopeAliasError<br/>• ContextError<br/>• PersistenceError"]
    
    Contract["Contract Errors<br/>• ScopeContractError<br/>• InputError<br/>• BusinessError<br/>• SystemError"]
    
    Domain -->|Extension Functions| App
    App -->|BaseErrorMapper| Contract
    
    classDef domain fill:#e8f5e8,stroke:#4caf50,stroke-width:3px
    classDef application fill:#e3f2fd,stroke:#2196f3,stroke-width:3px
    classDef contract fill:#fff3e0,stroke:#ff9800,stroke-width:3px
    
    class Domain domain
    class App application
    class Contract contract
```

## Error Translation Patterns

### Use Case Error Translation

Systematic error translation from service-specific errors to use case errors:

```mermaid
flowchart LR
    subgraph "Service Errors"
        SE1[TitleValidationError]
        SE2[ScopeHierarchyError]
        SE3[ScopeUniquenessError]
        SE4[RepositoryError]
    end

    subgraph "Translation Layer"
        TL[Error Mapper<br/>mapLeft]
    end

    subgraph "Use Case Errors"
        UE1[TitleValidationFailed]
        UE2[HierarchyViolationFailed]
        UE3[DuplicateTitleFailed]
        UE4[ParentNotFound]
        UE5[SaveFailure]
    end

    SE1 -->|mapLeft| UE1
    SE2 -->|mapLeft| UE2
    SE3 -->|mapLeft| UE3
    SE4 -->|mapLeft| UE4
    SE4 -->|mapLeft| UE5

    classDef service fill:#e8f5e9,stroke:#4caf50
    classDef usecase fill:#e3f2fd,stroke:#2196f3
    classDef mapper fill:#fff3e0,stroke:#ff9800

    class SE1,SE2,SE3,SE4 service
    class UE1,UE2,UE3,UE4,UE5 usecase
    class TL mapper
```

**Translation Pattern:**
```kotlin
// General pattern for error translation
applicationService.operation(input)
    .mapLeft { serviceError ->
        UseCaseError.SpecificError(serviceError)
    }
```

**Key Points:**
- Service errors are wrapped in use case-specific error types
- Context is preserved during translation
- Use `mapLeft` from Arrow's Either for functional error handling
- Each use case defines its own error hierarchy

### Domain to Application Mapping

Extension functions provide systematic error mapping between layers while preserving context:

```mermaid
flowchart TB
    subgraph "Mapping Strategy"
        D[Domain Error] --> EF[Extension Function]
        EF --> A[Application Error]

        D2[DomainPersistenceError] --> EF2[.toApplicationError()]
        EF2 --> A2[AppPersistenceError]

        D3[DomainScopeError] --> EF3[.toApplicationError()]
        EF3 --> A3[AppScopeError]
    end

    subgraph "Context Preservation"
        CP1[operation: String]
        CP2[entityType: String]
        CP3[entityId: String]
        CP4[reason: String]

        D2 -.->|preserves| CP1
        D2 -.->|preserves| CP2
        D2 -.->|preserves| CP3
        D2 -.->|preserves| CP4
    end

    classDef domain fill:#e8f5e9
    classDef application fill:#e3f2fd
    classDef extension fill:#fff3e0

    class D,D2,D3 domain
    class A,A2,A3 application
    class EF,EF2,EF3 extension
```

**Key Pattern:**
- Extension functions transform domain types to primitives
- All contextual information is preserved during mapping
- Mapping is exhaustive using `when` expressions

### Application to Contract Mapping

BaseErrorMapper pattern for contract boundaries:

```kotlin
class ApplicationErrorMapper(
    logger: Logger
) : BaseErrorMapper<ApplicationError, ScopeContractError>(logger) {
    
    override fun mapToContractError(
        domainError: ApplicationError
    ): ScopeContractError = when (domainError) {
        is AppScopeInputError.TitleEmpty -> 
            ScopeContractError.InputError.InvalidTitle(
                title = domainError.attemptedValue,
                validationFailure = ScopeContractError.TitleValidationFailure.Empty,
            )
        
        is AppScopeInputError.TitleTooShort -> 
            ScopeContractError.InputError.InvalidTitle(
                title = domainError.attemptedValue,
                validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                    minimumLength = domainError.minimumLength,
                    actualLength = domainError.attemptedValue.length,
                ),
            )
        
        else -> handleUnmappedError(
            domainError,
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management"
            ),
        )
    }
}
```

## ValidationResult Pattern

Comprehensive validation with error accumulation:

```kotlin
sealed class ValidationResult<out T> {
    data class Success<T>(val value: T) : ValidationResult<T>()
    data class Failure<T>(
        val errors: NonEmptyList<DomainError>
    ) : ValidationResult<T>()
    
    // Comprehensive validation with error accumulation
    fun <U, V> combine(
        other: ValidationResult<U>, 
        f: (T, U) -> V
    ): ValidationResult<V>
}

// Extension functions for ergonomic usage
fun <T> T.validationSuccess(): ValidationResult<T> = 
    ValidationResult.Success(this)

fun <T> DomainError.validationFailure(): ValidationResult<T> = 
    ValidationResult.Failure(nonEmptyListOf(this))

fun <T> List<ValidationResult<T>>.sequence(): ValidationResult<List<T>>
```

### Usage Example

```kotlin
suspend fun validateScopeCreation(
    title: String,
    description: String?,
    parentId: ScopeId?
): ValidationResult<Unit> {
    val validations = listOf(
        ScopeTitle.create(title).toValidationResult().map { },
        ScopeDescription.create(description).toValidationResult().map { },
        validateHierarchyDepth(parentId).toValidationResult(),
        validateChildrenLimit(parentId).toValidationResult(),
        validateTitleUniqueness(title, parentId).toValidationResult()
    )

    return validations.sequence().map { }
}
```

## Error Mapping Boundaries

### Context-Specific Mapping

Map errors based on the specific operation context:

```kotlin
class CreateScopeHandler {
    override suspend fun invoke(input: CreateScope) = either {
        // Context-specific error for parent validation
        val parent = repository.findById(input.parentId)
            .mapLeft { error ->
                ApplicationError.ScopeHierarchyError.ParentNotFound(
                    scopeId = "new",
                    parentId = input.parentId
                )
            }.bind()
        
        // Context-specific error for duplicate title
        val exists = repository.existsByTitle(input.title)
            .mapLeft { error ->
                ApplicationError.ScopeUniquenessError.DuplicateTitle(
                    title = input.title,
                    parentScopeId = input.parentId,
                    existingScopeId = "unknown"
                )
            }.bind()
    }
}
```

### Cross-Context Error Mapping

Handle errors across bounded contexts:

```kotlin
class EventStoreToScopeErrorMapper(
    logger: Logger
) : BaseCrossContextErrorMapper<EventStoreError, ScopesError>(logger) {
    
    override fun mapCrossContext(
        sourceError: EventStoreError
    ): ScopesError = when (sourceError) {
        is EventStoreError.ConnectionError -> 
            PersistenceError.StorageUnavailable(
                operation = "event-store-access",
                cause = sourceError.cause?.message
            )
        
        is EventStoreError.SerializationError -> 
            PersistenceError.DataCorruption(
                entityType = "event",
                entityId = sourceError.eventId,
                reason = "Event serialization failed: ${sourceError.details}"
            )
        
        else -> handleUnmappedCrossContextError(
            sourceError,
            PersistenceError.StorageUnavailable(
                operation = "cross-context-mapping",
                cause = "Unmapped EventStore error: ${sourceError::class.simpleName}"
            )
        )
    }
}
```

## Testing Error Handling

### Specification Testing

Test that error mappings preserve important information:

```kotlin
class ErrorMappingSpecificationTest : DescribeSpec({
    describe("Error mapping specifications") {
        it("should preserve error context during mapping") {
            val domainError = DomainScopeInputError.TitleError.TooShort(
                occurredAt = Clock.System.now(),
                attemptedValue = "ab",
                minimumLength = 3
            )
            
            val applicationError = domainError.toApplicationError() 
                as AppScopeInputError.TitleTooShort
            
            applicationError.attemptedValue shouldBe "ab"
            applicationError.minimumLength shouldBe 3
        }
        
        it("should fail fast for unmapped error types") {
            // Test that unmapped errors throw meaningful exceptions
            // rather than returning fallback errors
        }
    }
})
```

### Service-Specific Error Testing

Verify error translation and context preservation:

```kotlin
describe("title validation error translation") {
    it("should translate TitleTooShort to ValidationFailed") {
        val command = CreateScope(
            title = "ab",
            description = "Test description",
            parentId = null,
            metadata = emptyMap()
        )

        // Use test double instead of mock
        val fakeValidationService = object : ApplicationScopeValidationService {
            override suspend fun validateTitleFormat(title: String) =
                TitleValidationError.TitleTooShort(3, 2, "ab").left()
            // Implement other required methods as needed
        }

        val handler = CreateScopeHandler(repository, fakeValidationService)
        val result = handler.invoke(command)

        result.isLeft() shouldBe true
        val error = result.leftOrNull()
            .shouldBeInstanceOf<CreateScopeError.TitleValidationFailed>()
        val titleError = error.titleError
            .shouldBeInstanceOf<TitleValidationError.TitleTooShort>()
        titleError.minLength shouldBe 3
        titleError.actualLength shouldBe 2
    }
}
```

## Best Practices

### 1. Error Information Preservation

- **Never lose context**: Important error details must be preserved across mappings
- **Convert complex types**: Domain value objects → strings at application boundary  
- **Maintain error categories**: Validation errors remain validation errors across layers
- **Include actionable information**: Error messages should guide resolution

### 2. Mapping Strategy Selection

**Use Extension Functions when:**
- Mapping domain → application errors
- No additional context is required
- Mappings are reusable across handlers

**Use ErrorMapper Classes when:**
- Mapping application → contract errors
- Logging and fallback handling is needed
- Cross-context error translation is required

**Use Context-Specific Mapping when:**
- Error requires additional context from the operation
- Multiple domain errors map to the same application error
- Business logic affects error interpretation

### 3. Evolution and Maintenance  

**Adding New Error Types:**
1. Add new error type to domain layer
2. Compilation will fail at mapping points (fail-fast design)
3. Add explicit mapping in extension functions/mappers
4. Update tests to verify mapping behavior
5. Never use catch-all fallbacks that mask new error types

**Modifying Existing Mappings:**
1. Ensure backward compatibility at contract boundaries
2. Update tests to reflect new mapping behavior
3. Consider deprecation strategy for contract changes
4. Log mapping changes for observability

### Error Handling Checklist

When implementing error handling:

- [ ] **Domain Errors**: Sealed class hierarchies with rich context
- [ ] **Extension Functions**: Domain → Application mapping preserves context  
- [ ] **Error Mappers**: Application → Contract mapping with fallback handling
- [ ] **Fail-Fast**: Unmapped errors throw exceptions rather than fallback silently
- [ ] **Testing**: Specification tests verify mapping correctness
- [ ] **Logging**: Unmapped errors are logged with full context
- [ ] **Documentation**: Error handling rationale is documented
- [ ] **Architecture**: Konsist rules enforce mapping patterns

## Related Documentation

- [Clean Architecture](../../explanation/clean-architecture.md) - Layer responsibilities
- [Testing Patterns](./testing.md) - Error testing strategies
- [Domain-Driven Design](../../explanation/domain-driven-design.md) - DDD patterns
