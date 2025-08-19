# Coding Standards

This document outlines the coding standards and conventions for the Scopes project based on the current implementation, ensuring consistency, maintainability, and quality across the codebase.

## Core Principles

- **Type Safety First**: Use strongly-typed domain identifiers (`ScopeId`) instead of raw strings
- **Functional Error Handling**: Arrow Either for explicit error propagation, ValidationResult for error accumulation
- **Service-Specific Error Context**: Each validation service returns specific error types with rich context
- **Clean Architecture**: Strict layer separation with dependency rules enforced by Konsist tests
- **Domain-Driven Design**: Pure domain layer with repository-dependent validation in application layer

### Language and Communication

- **Code and Comments**: Always in English
- **Documentation**: Follow [Diátaxis framework](../explanation/adr/0006-adopt-diataxis-documentation-framework.md)
- **Commit Messages**: Use conventional commit format

## Current Error Handling Architecture

### Error Hierarchy

```kotlin
// Base domain error
sealed class DomainError

// Domain validation errors
sealed class ScopeValidationError : DomainError() {
    object EmptyScopeTitle : ScopeValidationError()
    object ScopeTitleTooShort : ScopeValidationError()
    data class ScopeTitleTooLong(val maxLength: Int, val actualLength: Int) : ScopeValidationError()
    object ScopeTitleContainsNewline : ScopeValidationError()
    data class ScopeDescriptionTooLong(val maxLength: Int, val actualLength: Int) : ScopeValidationError()
    data class ScopeInvalidFormat(val field: String, val expected: String) : ScopeValidationError()
}

sealed class ScopeError : DomainError() {
    object ScopeNotFound : ScopeError()
    data class InvalidTitle(val reason: String) : ScopeError()
    data class InvalidDescription(val reason: String) : ScopeError()
    data class InvalidParent(val parentId: ScopeId, val reason: String) : ScopeError()
    data class CircularReference(val scopeId: ScopeId, val parentId: ScopeId) : ScopeError()
    object SelfParenting : ScopeError()
}

// Application layer errors (do NOT extend DomainError)
sealed class TitleValidationError {
    object EmptyTitle : TitleValidationError()
    data class TitleTooShort(val minLength: Int, val actualLength: Int, val title: String) : TitleValidationError()
    data class TitleTooLong(val maxLength: Int, val actualLength: Int, val title: String) : TitleValidationError()
    data class InvalidCharacters(val title: String, val invalidCharacters: Set<Char>, val position: Int) : TitleValidationError()
}

// Note: Application errors are translated to domain errors at the application/domain boundary
// This ensures callers receive appropriate domain-level errors while maintaining layer separation

// Hierarchy validation errors (matching actual implementation)
sealed class ScopeHierarchyError : ConceptualModelError() {
    data class MaxDepthExceeded(val occurredAt: Instant, val scopeId: ScopeId, val attemptedDepth: Int, val maximumDepth: Int) : ScopeHierarchyError()
    data class MaxChildrenExceeded(val occurredAt: Instant, val parentScopeId: ScopeId, val currentChildrenCount: Int, val maximumChildren: Int) : ScopeHierarchyError()
    data class CircularReference(val occurredAt: Instant, val scopeId: ScopeId, val cyclePath: List<ScopeId>) : ScopeHierarchyError()
    data class SelfParenting(val occurredAt: Instant, val scopeId: ScopeId) : ScopeHierarchyError()
}

// Uniqueness validation (matching actual implementation)
// Title uniqueness is enforced at ALL levels including root level
sealed class ScopeUniquenessError : ConceptualModelError() {
    data class DuplicateTitle(
        val occurredAt: Instant,
        val title: String,
        val parentScopeId: ScopeId?,
        val existingScopeId: ScopeId
    ) : ScopeUniquenessError()
}

sealed class ApplicationValidationError {
    sealed class InputValidationError : ApplicationValidationError() {
        data class InvalidFieldFormat(
            val fieldName: String,
            val expectedFormat: String,
            val actualValue: String,
            val validationRule: String
        ) : InputValidationError()
        
        data class MissingRequiredField(
            val fieldName: String,
            val entityType: String,
            val context: String? = null
        ) : InputValidationError()
    }
}
```

### ValidationResult Pattern

```kotlin
// For accumulating multiple validation errors
sealed class ValidationResult<out T> {
    data class Success<T>(val value: T) : ValidationResult<T>()
    data class Failure(val errors: NonEmptyList<DomainError>) : ValidationResult<Nothing>()
}

// Extension functions for ergonomic usage
fun <T> T.validationSuccess(): ValidationResult<T> = ValidationResult.Success(this)
fun <T> DomainError.validationFailure(): ValidationResult<T> = ValidationResult.Failure(nonEmptyListOf(this))

// Sequencing multiple validations
fun <T> List<ValidationResult<T>>.sequence(): ValidationResult<List<T>>
```

### Use Case Error Translation Pattern

```kotlin
// Use case-specific errors with detailed context (current implementation)
sealed class CreateScopeError {
    data object ParentNotFound : CreateScopeError()
    data class ValidationFailed(val field: String, val reason: String) : CreateScopeError()
    data class DomainRuleViolation(val domainError: DomainError) : CreateScopeError()
    data class SaveFailure(val saveError: SaveScopeError) : CreateScopeError()
    data class ExistenceCheckFailure(val existsError: ExistsScopeError) : CreateScopeError()
    data class CountFailure(val countError: CountScopeError) : CreateScopeError()
    data class HierarchyTraversalFailure(val findError: FindScopeError) : CreateScopeError()
    data class HierarchyDepthExceeded(val maxDepth: Int, val currentDepth: Int) : CreateScopeError()
    data class MaxChildrenExceeded(val parentId: ScopeId, val maxChildren: Int) : CreateScopeError()
    
    // Service-specific error mappings
    data class TitleValidationFailed(val titleError: ScopeInputError.TitleError) : CreateScopeError()
    data class HierarchyViolationFailed(val hierarchyError: ScopeHierarchyError) : CreateScopeError()
    data class DuplicateTitleFailed(val uniquenessError: ScopeUniquenessError) : CreateScopeError()
}

// Translation in use case handlers
private fun validateTitleWithServiceErrors(title: String): Either<CreateScopeError, Unit> =
    applicationScopeValidationService.validateTitleFormat(title)
        .mapLeft { titleError -> CreateScopeError.ValidationFailed("title", titleError.toString()) }

private fun validateHierarchyWithServiceErrors(parentId: ScopeId?): Either<CreateScopeError, Unit> =
    applicationScopeValidationService.validateHierarchyConstraints(parentId)
        .mapLeft { domainError -> 
            when (domainError) {
                is ScopeBusinessRuleViolation -> CreateScopeError.ValidationFailed("hierarchy", domainError.toString())
                is DomainInfrastructureError -> CreateScopeError.ValidationFailed("infrastructure", "Repository error: ${domainError.repositoryError}")
                else -> CreateScopeError.ValidationFailed("unknown", domainError.toString())
            }
        }
```

## Strongly-Typed Domain Identifiers

### ScopeId Value Object

```kotlin
@JvmInline
value class ScopeId private constructor(private val value: String) {
    companion object {
        fun generate(): ScopeId = ScopeId(Ulid.fast().toString())
        fun from(value: String): ScopeId = ScopeId(value)
    }
    
    override fun toString(): String = value
}

// ✅ Good: Use ScopeId everywhere
data class CircularReference(
    val scopeId: ScopeId,  // Not String
    val parentId: ScopeId,  // Not String
    val cyclePath: List<ScopeId>  // Not List<String>
)

// ❌ Bad: Raw strings for domain identifiers
data class CircularReference(
    val scopeId: String,
    val parentId: String,
    val cyclePath: List<String>
)
```

## Validation Service Patterns

### Service-Specific Validation Methods

```kotlin
class ApplicationScopeValidationService(
    private val repository: ScopeRepository
) {
    // Returns specific error types with rich context
    fun validateTitleFormat(title: String): Either<TitleValidationError, Unit> {
        val trimmedTitle = title.trim()
        
        if (trimmedTitle.isBlank()) {
            return TitleValidationError.EmptyTitle.left()
        }
        
        if (trimmedTitle.length < ScopeTitle.MIN_LENGTH) {
            return TitleValidationError.TitleTooShort(
                minLength = ScopeTitle.MIN_LENGTH,
                actualLength = trimmedTitle.length,
                title = trimmedTitle
            ).left()
        }
        
        return Unit.right()
    }
    
    // Repository-dependent validations return domain errors (may include infrastructure errors)
    suspend fun validateHierarchyConstraints(parentId: ScopeId?): Either<DomainError, Unit> = either {
        if (parentId == null) return@either
        
        val depth = repository.findHierarchyDepth(parentId)
            .mapLeft { repositoryError -> 
                DomainInfrastructureError(
                    repositoryError = RepositoryError.DatabaseError(
                        "Failed to find hierarchy depth: operation='findHierarchyDepth', parentId=${parentId}",
                        causeClass = repositoryError::class,
                        causeMessage = repositoryError.toString()
                    )
                )
            }
            .bind()
        
        ensure(depth < MAX_HIERARCHY_DEPTH) {
            ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(
                maxDepth = MAX_HIERARCHY_DEPTH,
                actualDepth = depth + 1
            )
        }
    }
}
```

### Error Mapping Approach

#### Context-Specific Error Mapping

Instead of a centralized mapper, use context-specific error mappings in handlers:

```kotlin
// In handler: Map errors based on specific context
val parentScope = repository.findById(parentId)
    .mapLeft { error ->
        // Context-specific error for parent not found
        ApplicationError.ScopeHierarchyError.ParentNotFound(
            scopeId = "new",
            parentId = parentId
        )
    }.bind()
```

#### Extension Functions for Common Mappings

Common error mappings are provided as extension functions:

```kotlin
// Extension functions in ErrorMappingExtensions.kt
fun PersistenceError.toApplicationError(): ApplicationError = when (this) {
    is StorageUnavailable -> ApplicationError.PersistenceError.StorageUnavailable(...)
    is DataCorruption -> ApplicationError.PersistenceError.DataCorruption(...)
    // ...
}

fun ScopeInputError.toApplicationError(): ApplicationError = when (this) {
    is IdError.InvalidFormat -> ApplicationError.ScopeInputError.IdInvalidFormat(...)
    is TitleError.TooLong -> ApplicationError.ScopeInputError.TitleTooLong(...)
    // ...
}

// Usage in handlers
repository.save(entity)
    .mapLeft { error -> 
        // Use extension function for common persistence errors
        error.toApplicationError()
    }.bind()
```

#### Benefits of This Approach

1. **Contextual Clarity**: Errors are more meaningful when mapped in context
2. **Flexibility**: Each handler can provide specific error messages
3. **Reusability**: Common patterns are available as extension functions
4. **Maintainability**: No central mapper to update for every new error
5. **Type Safety**: Compiler ensures all error cases are handled

## Use Case Handler Patterns

### Transaction Boundary and Validation Pipeline

```kotlin
class CreateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val applicationScopeValidationService: ApplicationScopeValidationService
) : UseCase<CreateScope, CreateScopeError, CreateScopeResult> {

    override suspend operator fun invoke(input: CreateScope): Either<CreateScopeError, CreateScopeResult> = either {
        // Step 1: Parse and validate parent exists
        val parentId = validateParentExists(input.parentId).bind()
        
        // Step 2: Service-specific validations with error translation
        validateTitleWithServiceErrors(input.title).bind()
        validateHierarchyWithServiceErrors(parentId).bind()
        validateUniquenessWithServiceErrors(input.title, parentId).bind()
        
        // Step 3: Create domain entity
        val scope = createScopeEntity(input.title, input.description, parentId, input.metadata).bind()
        
        // Step 4: Persist entity
        val savedScope = saveScopeEntity(scope).bind()
        
        // Step 5: Map to DTO
        ScopeMapper.toCreateScopeResult(savedScope)
    }
}
```

## Repository Error Handling

### Error Handling Best Practices

```kotlin
// Repository methods return operation-specific error types
interface ScopeRepository {
    suspend fun save(scope: Scope): Either<SaveScopeError, Scope>
    suspend fun existsById(id: ScopeId): Either<ExistsScopeError, Boolean>
    suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<ExistsScopeError, Boolean>
    suspend fun countByParentId(parentId: ScopeId): Either<CountScopeError, Int>
    suspend fun findHierarchyDepth(scopeId: ScopeId): Either<FindScopeError, Int>
}

// Operation-specific error types
sealed class SaveScopeError : DomainError() {
    data class ConcurrentModification(val scopeId: ScopeId, val expectedVersion: Int, val actualVersion: Int)
    data class ValidationFailed(val violations: List<String>)
    data class NetworkError(val message: String, val cause: Throwable?)
    // ... other save-specific errors
}

sealed class ExistsScopeError : DomainError() {
    data class IndexCorruption(val scopeId: ScopeId, val message: String)
    data class QueryTimeout(val operation: String, val timeoutMs: Long)
    // ... other exists-specific errors
}

sealed class CountScopeError : DomainError() {
    data class IndexUnavailable(val parentId: ScopeId, val reason: String)
    data class QueryTimeout(val operation: String, val timeoutMs: Long)
    // ... other count-specific errors
}

sealed class FindScopeError : DomainError() {
    data class HierarchyCorruption(val scopeId: ScopeId, val message: String)
    data class DepthCalculationError(val scopeId: ScopeId, val cause: String)
    // ... other find-specific errors
}
```

## Testing Patterns

### Service-Specific Error Testing

```kotlin
// Import application-layer error types
import io.github.kamiazya.scopes.domain.error.TitleValidationError

class CreateScopeHandlerServiceErrorIntegrationTest : DescribeSpec({
    describe("title validation error translation") {
        it("should translate TitleValidationError.TitleTooShort to ValidationFailed") {
            val command = CreateScope(
                title = "ab",
                description = "Test description",
                parentId = null,
                metadata = emptyMap()
            )

            coEvery { mockValidationService.validateTitleFormat("ab") } returns 
                TitleValidationError.TitleTooShort(
                    minLength = 3,
                    actualLength = 2,
                    title = "ab"
                ).left()
            
            val result = handler.invoke(command)
            
            result.isLeft() shouldBe true
            val error = result.leftOrNull().shouldBeInstanceOf<CreateScopeError.ValidationFailed>()
            error.field shouldBe "title"
            error.reason shouldContain "too short"
        }
    }
})
```

### Architecture Testing with Konsist

```kotlin
class UseCaseArchitectureTest : StringSpec({
    "use case handlers should have proper naming" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .withNameEndingWith("Handler")
            .assert { it.name.endsWith("Handler") }
    }
    
    "use case errors should be sealed classes" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .withNameEndingWith("Error")
            .assert { it.isSealed }
    }
})
```

## File Organization

### Current Package Structure

```
scopes/
├── domain/
│   ├── entity/              # Scope.kt
│   ├── valueobject/         # ScopeId.kt, ScopeTitle.kt
│   ├── repository/          # ScopeRepository.kt
│   ├── service/             # Domain services (pure functions)
│   ├── error/               # All error hierarchies
│   └── util/                # TitleNormalizer.kt
├── application/
│   ├── usecase/
│   │   ├── command/         # CreateScope.kt
│   │   ├── handler/         # CreateScopeHandler.kt
│   │   └── error/           # CreateScopeError.kt
│   └── service/             # ApplicationScopeValidationService.kt
├── infrastructure/
│   └── repository/          # Repository implementations
└── presentation-cli/
    ├── command/             # CLI commands
    └── mapper/              # ScopeMapper.kt
```

## Kotlin Style Guidelines

### Naming Conventions

```kotlin
// ✅ Good: Clear, descriptive names
class CreateScopeHandler
data class ScopeCreationRequest
sealed class DomainError

// ❌ Bad: Abbreviations and unclear names
class CSHandler
data class ScopeReq
sealed class Err
```

### Function Composition

```kotlin
// ✅ Good: Function composition with validated factory method
fun createScope(request: CreateScopeRequest): Either<DomainError, Scope> =
    validateParent(request.parentId)
        .flatMap { _ ->
            // Use validated factory that enforces domain invariants
            Scope.create(
                title = request.title,
                description = request.description,
                parentId = request.parentId,
                metadata = request.metadata
            ).mapLeft { validationError -> 
                // Map ScopeValidationError to DomainError
                validationError as DomainError
            }
        }

// ❌ Bad: Direct construction bypasses domain invariants  
fun createScopeUnsafe(request: CreateScopeRequest): Either<DomainError, Scope> =
    validateTitle(request.title)
        .flatMap { title -> validateParent(request.parentId) }
        .map { _ ->
            // This bypasses ScopeTitle and ScopeDescription validation!
            Scope(
                id = ScopeId.generate(),
                title = request.title, // Should be ScopeTitle, not String
                description = request.description, // Should be ScopeDescription?, not String?
                parentId = request.parentId,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
```

## Quality Assurance

### Automated Validation

```bash
# Run architecture tests to verify compliance
./gradlew konsistTest

# Run all quality checks
./gradlew check

# Format code
./gradlew ktlintFormat
```

### Git Hooks (lefthook.yml)

```yaml
pre-commit:
  commands:
    ktlint:
      run: ./gradlew ktlintFormat && git add -u
    tests:
      run: ./gradlew test konsistTest
    detekt:
      run: ./gradlew detekt
```

## Flat Structure Pattern (Functional Style)

### Core Principle: Linear Control Flow

Use Arrow's `either` blocks with `ensure()`, `ensureNotNull()` to create flat, linear control flow instead of nested if-else statements.

### Pattern Guidelines

1. **Use `ensure()` for validation checks**
   - Replace `if (!condition) raise(error)` with `ensure(condition) { error }`
   - Makes the happy path more visible
   - **NEVER use `raise()` directly** - always prefer `ensure()` or `ensureNotNull()`

2. **Use `ensureNotNull()` for null checks**
   - Replace `if (value == null) raise(error)` with `ensureNotNull(value) { error }`
   - Provides smart casting after the check

3. **Use `forEach` instead of `for` loops**
   - More functional and composable
   - Works well with `either` blocks

4. **Single `either` block per function**
   - Avoid nested `either` blocks
   - Keep error handling flat and linear

5. **Special cases for `ensure(false)`**
   - Only use `ensure(false) { error }` when you need to always fail (e.g., after exhausting retries)
   - This is equivalent to `raise(error)` but maintains consistency with the ensure pattern

### Before (Nested Structure - Avoid This)
```kotlin
suspend fun validateHierarchyConsistency(
    parentId: ScopeId,
    childIds: List<ScopeId>
): Either<Error, Unit> = either {
    val parentExists = repository.existsById(parentId)
        .mapLeft { error -> /* map error */ }
        .bind()
    
    if (!parentExists) {
        raise(Error.ParentNotFound(parentId))  // ❌ Avoid raise()
    }
    
    for (childId in childIds) {  // ❌ Avoid traditional for loops
        val childExists = repository.existsById(childId)
            .mapLeft { error -> /* map error */ }
            .bind()
        
        if (!childExists) {  // ❌ Avoid if-else for validation
            raise(Error.ChildNotFound(childId))
        }
    }
}
```

### After (Flat Structure)
```kotlin
suspend fun validateHierarchyConsistency(
    parentId: ScopeId,
    childIds: List<ScopeId>
): Either<Error, Unit> = either {
    val parentExists = repository.existsById(parentId)
        .mapLeft { /* map error */ }
        .bind()
    
    ensure(parentExists) {
        Error.ParentNotFound(parentId)
    }
    
    childIds.forEach { childId ->
        val childExists = repository.existsById(childId)
            .mapLeft { /* map error */ }
            .bind()
        
        ensure(childExists) {
            Error.ChildNotFound(childId)
        }
    }
}
```

### UseCase Handler Pattern

```kotlin
class CreateScopeHandler(
    private val repository: ScopeRepository,
    private val logger: Logger
) : UseCase<Input, Error, Result> {
    
    override suspend fun invoke(input: Input): Either<Error, Result> = either {
        // Log at the start
        logger.info("Starting operation", mapOf("input" to input))
        
        // Linear validation steps with ensure()
        val validatedTitle = validateTitle(input.title).bind()
        
        val parentExists = repository.existsById(input.parentId)
            .mapLeft { mapError(it) }
            .bind()
        
        ensure(parentExists) {
            Error.ParentNotFound(input.parentId)
        }
        
        // Create and save entity
        val entity = createEntity(validatedTitle, input.parentId)
        val saved = repository.save(entity)
            .mapLeft { mapError(it) }
            .bind()
        
        logger.info("Operation completed", mapOf("id" to saved.id))
        
        // Return result
        mapToResult(saved)
    }
}
```

### Service Pattern

```kotlin
class ValidationService(
    private val repository: Repository
) {
    suspend fun validate(data: Data): Either<Error, Unit> = either {
        // Single flat either block
        val exists = repository.exists(data.id)
            .mapLeft { mapError(it) }
            .bind()
        
        ensure(exists) {
            Error.NotFound(data.id)
        }
        
        ensure(data.value > 0) {
            Error.InvalidValue(data.value)
        }
        
        // More validations in linear fashion
    }
}
```

## Summary

### Current Implementation Patterns ✅

- **Strongly-typed domain identifiers** (ScopeId instead of String)
- **Service-specific error contexts** (ScopeInputError, ScopeUniquenessError, ScopeHierarchyError, ApplicationValidationError)
- **Error translation in use case handlers** (service errors → use case errors)
- **ValidationResult for error accumulation** with extension functions
- **Repository-dependent validation** in application layer
- **Functional error handling** with Arrow Either
- **Flat structure with ensure()/ensureNotNull()** for linear control flow
- **Functional iteration** with forEach instead of for loops
- **Architecture testing** with Konsist
- **Context-aware error mapping** with extension functions
- **Logging at boundaries** with structured context

### Avoid ❌

- Raw strings for domain identifiers
- Generic or context-free error types
- Repository access in domain entities
- Exception-based error handling
- Violating Clean Architecture layer dependencies
- Missing error translation between layers
- Incomplete error context information
- **Using raise() directly** (use ensure()/ensureNotNull() instead)
- Nested if-else chains (use ensure() instead)
- Traditional for loops (use forEach/map/filter)
- Multiple nested either blocks
- Deep nesting in general
