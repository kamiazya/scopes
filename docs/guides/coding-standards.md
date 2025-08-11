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

// Service-specific error hierarchies
sealed class TitleValidationError : DomainError() {
    object EmptyTitle : TitleValidationError()
    data class TitleTooShort(val minLength: Int, val actualLength: Int, val title: String)
    data class TitleTooLong(val maxLength: Int, val actualLength: Int, val title: String)
    data class InvalidCharacters(val title: String, val invalidCharacters: Set<Char>, val position: Int)
}

sealed class ScopeBusinessRuleError : DomainError() {
    data class MaxDepthExceeded(val maxDepth: Int, val actualDepth: Int, val scopeId: ScopeId, val parentPath: List<ScopeId>)
    data class MaxChildrenExceeded(val maxChildren: Int, val currentChildren: Int, val parentId: ScopeId, val attemptedOperation: String)
    data class DuplicateScope(val duplicateTitle: String, val parentId: ScopeId, val existingScopeId: ScopeId, val normalizedTitle: String)
}

sealed class UniquenessValidationError : DomainError() {
    data class DuplicateTitle(val title: String, val normalizedTitle: String, val parentId: String?, val existingScopeId: String)
}
```

### ValidationResult Pattern

```kotlin
// For accumulating multiple validation errors
sealed class ValidationResult<out T> {
    data class Success<T>(val value: T) : ValidationResult<T>()
    data class Failure<T>(val errors: NonEmptyList<DomainError>) : ValidationResult<T>()
}

// Extension functions for ergonomic usage
fun <T> T.validationSuccess(): ValidationResult<T> = ValidationResult.Success(this)
fun <T> DomainError.validationFailure(): ValidationResult<T> = ValidationResult.Failure(nonEmptyListOf(this))

// Sequencing multiple validations
fun <T> List<ValidationResult<T>>.sequence(): ValidationResult<List<T>>
```

### Use Case Error Translation Pattern

```kotlin
// Use case-specific errors with service error context
sealed class CreateScopeError {
    data class TitleValidationFailed(val titleError: TitleValidationError) : CreateScopeError()
    data class BusinessRuleViolationFailed(val businessRuleError: ScopeBusinessRuleError) : CreateScopeError()
    data class DuplicateTitleFailed(val uniquenessError: UniquenessValidationError) : CreateScopeError()
    object ParentNotFound : CreateScopeError()
    data class SaveFailure(val repositoryError: SaveScopeError) : CreateScopeError()
}

// Translation in use case handlers
private fun validateTitleWithServiceErrors(title: String): Either<CreateScopeError, Unit> =
    applicationScopeValidationService.validateTitleFormat(title)
        .mapLeft { titleError -> CreateScopeError.TitleValidationFailed(titleError) }
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
    
    // Repository-dependent validations return business rule errors
    suspend fun validateHierarchyConstraints(parentId: ScopeId?): Either<ScopeBusinessRuleError, Unit> = either {
        if (parentId == null) return@either
        
        val depth = repository.findHierarchyDepth(parentId).bind()
        if (depth >= MAX_HIERARCHY_DEPTH) {
            raise(ScopeBusinessRuleError.MaxDepthExceeded(
                maxDepth = MAX_HIERARCHY_DEPTH,
                actualDepth = depth + 1,
                scopeId = parentId,
                parentPath = emptyList()
            ))
        }
    }
}
```

### Error Mapping Functions

```kotlin
// Centralized error translation
private fun mapBusinessRuleErrorToScopeError(error: ScopeBusinessRuleError, parentId: ScopeId?): ScopeError =
    when (error) {
        is ScopeBusinessRuleError.MaxDepthExceeded ->
            ScopeError.InvalidParent(
                parentId ?: ScopeId.generate(),
                "Maximum hierarchy depth (${error.maxDepth}) would be exceeded"
            )
        is ScopeBusinessRuleError.MaxChildrenExceeded ->
            ScopeError.InvalidParent(
                parentId ?: ScopeId.generate(), 
                "Maximum children limit (${error.maxChildren}) would be exceeded"
            )
        // ... other cases
    }
```

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

### Comprehensive Error Mapping

```kotlin
// Repository methods return specific error types
interface ScopeRepository {
    suspend fun findById(id: ScopeId): Either<FindScopeError, Scope?>
    suspend fun existsById(id: ScopeId): Either<ExistsScopeError, Boolean>
    suspend fun save(scope: Scope): Either<SaveScopeError, Scope>
    suspend fun countByParentId(parentId: ScopeId): Either<CountScopeError, Int>
}

// Detailed error mapping in validation services
private fun mapExistsScopeError(existsError: ExistsScopeError, parentId: ScopeId?): DomainError =
    when (existsError) {
        is ExistsScopeError.IndexCorruption -> {
            if (parentId != null) {
                ScopeError.InvalidParent(
                    parentId,
                    "Index corruption detected: ${existsError.message}. ScopeId: ${existsError.scopeId}"
                )
            } else {
                DomainInfrastructureError(
                    RepositoryError.DataIntegrityError(
                        "Index corruption in root-level check: ${existsError.message}",
                        cause = RuntimeException("Index corruption")
                    )
                )
            }
        }
        is ExistsScopeError.QueryTimeout -> 
            DomainInfrastructureError(
                RepositoryError.DatabaseError(
                    "Query timeout: operation='${existsError.operation}', timeout=${existsError.timeoutMs}ms",
                    RuntimeException("Query timeout: ${existsError.operation}")
                )
            )
        // ... other error mappings
    }
```

## Testing Patterns

### Service-Specific Error Testing

```kotlin
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
                TitleValidationError.TitleTooShort(3, 2, "ab").left()
            
            val result = handler.invoke(command)
            
            result.isLeft() shouldBe true
            val error = result.leftOrNull().shouldBeInstanceOf<CreateScopeError.TitleValidationFailed>()
            val titleError = error.titleError.shouldBeInstanceOf<TitleValidationError.TitleTooShort>()
            titleError.minLength shouldBe 3
            titleError.actualLength shouldBe 2
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
// ✅ Good: Function composition with Arrow Either
fun createScope(request: CreateScopeRequest): Either<DomainError, Scope> =
    validateTitle(request.title)
        .flatMap { title -> validateParent(request.parentId) }
        .map { _ ->
            Scope(
                id = ScopeId.generate(),
                title = request.title,
                description = request.description,
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

## Summary

### Current Implementation Patterns ✅

- **Strongly-typed domain identifiers** (ScopeId instead of String)
- **Service-specific error contexts** (TitleValidationError, ScopeBusinessRuleError)
- **Error translation in use case handlers** (service errors → use case errors)
- **ValidationResult for error accumulation** with extension functions
- **Repository-dependent validation** in application layer
- **Functional error handling** with Arrow Either
- **Architecture testing** with Konsist
- **Comprehensive error mapping** with detailed context

### Avoid ❌

- Raw strings for domain identifiers
- Generic or context-free error types
- Repository access in domain entities
- Exception-based error handling
- Violating Clean Architecture layer dependencies
- Missing error translation between layers
- Incomplete error context information
