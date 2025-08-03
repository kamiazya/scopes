# Coding Standards

This document defines the coding standards and conventions for the Scopes project. These standards are enforced through automated tools and should be followed by all contributors.


## General Principles

### Code Quality Philosophy

1. **Readability First**: Code is read more often than it's written
2. **Functional Purity**: Prefer pure functions and immutable data
3. **Explicit Over Implicit**: Make intentions clear through code
4. **Fail Fast**: Detect errors as early as possible
5. **Single Responsibility**: Each component should have one reason to change

### Language and Communication

- **Code and Comments**: Always in English
- **Documentation**: Follow [Diátaxis framework](../explanation/adr/0006-adopt-diataxis-documentation-framework.md)
- **Commit Messages**: Use conventional commit format

## Kotlin Coding Conventions

### Naming Conventions

#### Classes and Interfaces

      ```typescript
// ✅ Good: PascalCase with descriptive names
class ScopeRepository
interface DomainEventHandler
data class CreateScopeRequest
sealed class ValidationError

// ❌ Bad: Abbreviations and unclear names
class ScopeRepo
interface Handler
data class CSReq
sealed class ValErr
      ```typescript

#### Functions and Variables

      ```typescript
// ✅ Good: camelCase with verb-noun pattern
fun createScope(request: CreateScopeRequest): Result<Scope, DomainError>
fun validateScopeTitle(title: String): Result<String, ValidationError>
val currentTimestamp: Instant = Clock.System.now()

// ❌ Bad: Unclear or abbreviated names
fun create(req: Any): Any
fun validate(s: String): Boolean
val ts: Long = System.currentTimeMillis()
      ```typescript

#### Constants and Enums

      ```typescript
// ✅ Good: SCREAMING_SNAKE_CASE for constants
object ScopeConstants {
        const val MAX_TITLE_LENGTH = 200
        const val MIN_TITLE_LENGTH = 1
}

// ✅ Good: PascalCase for enum values
enum class ScopeStatus {
        Active,
        Archived,
        Deleted
}

// ❌ Bad: camelCase for constants
object ScopeConstants {
        const val maxTitleLength = 200
}
      ```typescript

### File Organization

#### Package Structure

      ```typescript
// ✅ Good: Clear package hierarchy
package com.kamiazya.scopes.domain.entity
package com.kamiazya.scopes.application.usecase
package com.kamiazya.scopes.infrastructure.repository.impl

// ❌ Bad: Flat or unclear structure
package com.kamiazya.scopes
package com.kamiazya.scopes.stuff
      ```typescript

#### Import Organization

      ```typescript
// ✅ Good: Organized imports
import java.time.Instant
import kotlin.collections.List

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.valueobject.ScopeId

// ❌ Bad: Wildcard imports in production code
import kotlin.collections.*
import com.kamiazya.scopes.domain.*
      ```typescript

### Code Formatting

#### Line Length and Wrapping

      ```typescript
// ✅ Good: Respect 120 character limit
fun createScopeWithValidation(
        title: String,
        description: String?,
        parentId: ScopeId?
): Result<Scope, ValidationError> = validateInput(title, description)
        .flatMap { createScope(it, parentId) }
        .map { scope -> scope.copy(createdAt = Clock.System.now()) }

// ❌ Bad: Long lines without proper wrapping
fun createScopeWithValidation(title: String, description: String?, parentId: ScopeId?): Result<Scope, ValidationError> = validateInput(title, description).flatMap { createScope(it, parentId) }.map { scope -> scope.copy(createdAt = Clock.System.now()) }
      ```typescript

#### Function Declarations

      ```typescript
// ✅ Good: Expression body for simple functions
fun isValidTitle(title: String): Boolean =
        title.isNotBlank() && title.length <= MAX_TITLE_LENGTH

// ✅ Good: Block body for complex functions
fun createScope(request: CreateScopeRequest): Result<Scope, DomainError> {
        return validateTitle(request.title)
            .flatMap { title ->
                validateParent(request.parentId)
                    .map { _ -> title }
            }
            .map { title ->
                Scope(
                    id = ScopeId.generate(),
                    title = title,
                    description = request.description,
                    parentId = request.parentId,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            }
}
      ```typescript

## Functional Programming Standards

### Functional Programming Principles

```mermaid
flowchart TD
        subgraph "Functional Programming Core"
            A[Immutable Data]
            B[Pure Functions]
            C[Function Composition]
            D[No Side Effects]
            E[Explicit State Management]

            A --> B
            B --> C
            C --> D
            D --> E
            E --> A
        end

        subgraph "Benefits"
            F[Predictable Behavior]
            G[Easy Testing]
            H[Concurrent Safety]
            I[Better Reasoning]
        end

        A --> F
        B --> G
        C --> H
        D --> I

        classDef principle fill:#f1f8e9,stroke:#689f38,stroke-width:3px
        classDef benefit fill:#e8f5e8,stroke:#4caf50,stroke-width:2px

        class A,B,C,D,E principle
        class F,G,H,I benefit
      ```typescript

### Immutability

#### Data Classes

      ```typescript
// ✅ Good: Immutable data class
data class Scope(
        val id: ScopeId,
        val title: String,
        val description: String?,
        val parentId: ScopeId?,
        val createdAt: Instant,
        val updatedAt: Instant
) {
        fun updateTitle(newTitle: String): Scope =
            copy(title = newTitle, updatedAt = Clock.System.now())
}

// ❌ Bad: Mutable data class
data class Scope(
        val id: ScopeId,
        var title: String,
        var description: String?
) {
        fun updateTitle(newTitle: String) {
            title = newTitle
        }
}
      ```typescript

#### Value Objects

      ```typescript
// ✅ Good: Immutable value object with inline class
@JvmInline
value class ScopeId private constructor(private val value: String) {
        companion object {
            fun generate(): ScopeId = ScopeId(Ulid.fast().toString())
            fun from(value: String): ScopeId {
                require(Ulid.isValid(value)) { "Invalid ULID format: $value" }
                return ScopeId(value)
            }
        }

        override fun toString(): String = value
}

// ❌ Bad: Mutable value object
class ScopeId {
        var value: String = ""

        fun setValue(newValue: String) {
            value = newValue
        }
}
      ```typescript

### Pure Functions

#### Function Purity

      ```typescript
// ✅ BEST: Pure function with Either for error handling
fun calculateScopeDepth(
        scopeId: ScopeId,
        allScopes: List<Scope>
): Either<ScopeError, Int> = either {
        val scope = allScopes.find { it.id == scopeId }
            ?: raise(ScopeError.NotFound(scopeId))

        tailrec fun calculateDepth(currentScope: Scope?, depth: Int): Int =
            when (currentScope?.parentId) {
                null -> depth
                else -> {
                    val parent = allScopes.find { it.id == currentScope.parentId }
                    calculateDepth(parent, depth + 1)
                }
            }

        ensure(calculateDepth(scope, 0) <= MAX_DEPTH) {
            ScopeError.DepthExceeded
        }

        calculateDepth(scope, 0)
}

// ✅ Good: Pure function - no side effects
fun isValidHierarchy(child: ScopeId, parent: ScopeId): Boolean =
        child != parent

// ❌ Bad: Impure function - has side effects
class ScopeService {
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun calculateScopeDepth(scope: Scope): Int {
            logger.info("Calculating depth for scope: ${scope.id}") // Side effect
            var depth = 0
            var current = scope
            // ... calculation with mutable state
            return depth
        }
}
      ```typescript

#### Function Composition with Arrow Either

      ```typescript
// ✅ BEST: Using either DSL for clean, sequential code
fun createValidatedScope(request: CreateScopeRequest): Either<DomainError, Scope> = either {
        val title = validateTitle(request.title).bind()
        validateDescription(request.description).bind()
        validateParentExists(request.parentId).bind()
        buildScope(title, request.description, request.parentId)
}

// ✅ BEST: Simple functions with either DSL
fun validateInput(input: String): Either<ValidationError, ValidatedInput> = either {
        ensure(input.isNotBlank()) { ValidationError.EmptyInput }
        ensure(input.length <= MAX_LENGTH) { ValidationError.TooLong }
        ValidatedInput(input.trim())
}

// ✅ OK: Using Arrow's combinators for specific cases
fun processScope(id: ScopeId): Either<DomainError, ProcessedScope> =
        findScope(id)
            .flatMap { scope -> validateScope(scope) }
            .map { validScope -> transform(validScope) }
            .mapLeft { error -> enrichError(error) }

// 📌 GUIDELINE: Prefer either DSL for most cases
// - Use `either { ... }` for sequential operations
// - Use `ensure` for validations within either blocks
// - Use `bind()` to unwrap Either values
// - Avoid nested flatMap chains when either DSL is clearer
      ```typescript

### Sealed Classes for Domain Modeling

      ```typescript
// ✅ Good: Sealed classes for domain states
sealed class ScopeCommand {
        data class CreateScope(
            val title: String,
            val description: String?,
            val parentId: ScopeId?
        ) : ScopeCommand()

        data class UpdateScope(
            val id: ScopeId,
            val title: String?,
            val description: String?
        ) : ScopeCommand()

        data class ArchiveScope(val id: ScopeId) : ScopeCommand()
}

sealed class ScopeEvent {
        data class ScopeCreated(val scope: Scope) : ScopeEvent()
        data class ScopeUpdated(val scope: Scope) : ScopeEvent()
        data class ScopeArchived(val scopeId: ScopeId) : ScopeEvent()
}

// Pattern matching with when expressions
fun handleCommand(command: ScopeCommand): Result<ScopeEvent, DomainError> =
        when (command) {
            is ScopeCommand.CreateScope -> createScope(command)
            is ScopeCommand.UpdateScope -> updateScope(command)
            is ScopeCommand.ArchiveScope -> archiveScope(command)
        }
      ```typescript

## Architecture Patterns

### Clean Architecture Overview

```mermaid
flowchart TD
        subgraph "Clean Architecture Layers"
            subgraph "Presentation Layer"
                CLI[CLI Commands]
                WEB[Web Controllers]
            end

            subgraph "Application Layer"
                UC[Use Cases]
                AS[Application Services]
            end

            subgraph "Domain Layer"
                ENT[Entities]
                VO[Value Objects]
                DOM_SRV[Domain Services]
                REPO_INT[Repository Interfaces]
            end

            subgraph "Infrastructure Layer"
                REPO_IMPL[Repository Implementations]
                DB[Database]
                EXT[External Services]
            end

            CLI --> UC
            WEB --> UC
            UC --> ENT
            UC --> DOM_SRV
            UC --> REPO_INT
            AS --> UC
            REPO_IMPL --> REPO_INT
            REPO_IMPL --> DB
            REPO_IMPL --> EXT
        end

        classDef presentation fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px
        classDef application fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
        classDef domain fill:#e8f5e8,stroke:#4caf50,stroke-width:3px
        classDef infrastructure fill:#fff3e0,stroke:#ff9800,stroke-width:2px

        class CLI,WEB presentation
        class UC,AS application
        class ENT,VO,DOM_SRV,REPO_INT domain
        class REPO_IMPL,DB,EXT infrastructure
      ```typescript

### Clean Architecture Layer Separation

#### Domain Layer

      ```typescript
// ✅ Good: Domain entity with business logic
data class Scope(
        val id: ScopeId,
        val title: String,
        val description: String?,
        val parentId: ScopeId?,
        val createdAt: Instant,
        val updatedAt: Instant
) {
        fun isChildOf(potentialParent: Scope): Boolean =
            parentId == potentialParent.id

        fun canBeParentOf(potentialChild: Scope): Boolean =
            potentialChild.parentId != id && id != potentialChild.id
}

// ✅ Good: Repository interface with Arrow Either
interface ScopeRepository {
        suspend fun findById(id: ScopeId): Either<RepositoryError, Scope?>
        suspend fun findByParentId(parentId: ScopeId): Either<RepositoryError, List<Scope>>
        suspend fun save(scope: Scope): Either<RepositoryError, Scope>
        suspend fun delete(id: ScopeId): Either<RepositoryError, Unit>
}
      ```typescript

#### Application Layer

      ```typescript
// ✅ Good: Use case with single responsibility and Arrow Either
class CreateScopeUseCase(
        private val scopeRepository: ScopeRepository,
        private val eventPublisher: DomainEventPublisher
) {
        suspend fun execute(request: CreateScopeRequest): Either<ApplicationError, CreateScopeResponse> = either {
            val validRequest = validateRequest(request)
                .mapLeft { ApplicationError.DomainError(it) }
                .bind()

            val scope = createScope(validRequest).bind()
            val savedScope = saveScope(scope).bind()

            publishEvent(ScopeCreated(savedScope))
            CreateScopeResponse(savedScope)
        }

        private fun validateRequest(request: CreateScopeRequest): Either<DomainError, CreateScopeRequest> = either {
            ensure(request.title.isNotBlank()) {
                DomainError.InvalidTitle("Title cannot be blank")
            }
            ensure(request.title.length <= MAX_TITLE_LENGTH) {
                DomainError.InvalidTitle("Title too long")
            }
            request
        }
}
      ```typescript

### Dependency Injection Patterns

      ```typescript
// ✅ Good: Constructor injection with interfaces
class ScopeService(
        private val scopeRepository: ScopeRepository,
        private val validationService: ScopeValidationService,
        private val eventPublisher: DomainEventPublisher
) {
        suspend fun createScope(request: CreateScopeRequest): Either<ApplicationError, Scope> = either {
            // Implementation using Either DSL
            val validRequest = validationService.validate(request).bind()
            val scope = Scope.create(validRequest).bind()
            scopeRepository.save(scope).bind()
        }
}

// ✅ Good: Factory pattern for complex construction
object ScopeServiceFactory {
        fun create(
            repository: ScopeRepository,
            publisher: DomainEventPublisher
        ): ScopeService = ScopeService(
            scopeRepository = repository,
            validationService = ScopeValidationService,
            eventPublisher = publisher
        )
}
      ```typescript

## Error Handling

### Arrow Either Pattern

```mermaid
sequenceDiagram
        participant Client
        participant Function
        participant Either
        participant NextFunction

        Client->>Function: Input

        alt Right Path (Success)
            Function->>Either: Right(value)
            Either->>Client: map/flatMap
            Client->>NextFunction: Composed operation
            NextFunction->>Client: Right result
        else Left Path (Error)
            Function->>Either: Left(error)
            Either->>Client: Error propagation
            Note over Client: Short-circuit evaluation
        end
      ```typescript

### Arrow Either Implementation

      ```typescript
// ✅ BEST: Arrow Either with DSL for explicit error handling
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

// Simple validation with either DSL
fun validateAge(age: Int): Either<ValidationError, Age> = either {
        ensure(age >= 0) { ValidationError.NegativeAge }
        ensure(age <= 150) { ValidationError.UnrealisticAge }
        Age(age)
}

// Composing multiple operations
fun registerUser(request: RegistrationRequest): Either<RegistrationError, User> = either {
        val email = validateEmail(request.email).bind()
        val age = validateAge(request.age).bind()
        val username = checkUsernameAvailable(request.username).bind()

        createUser(email, age, username)
}

// Handling results with fold
fun handleRegistration(request: RegistrationRequest) {
        registerUser(request).fold(
            ifLeft = { error -> showError(error) },
            ifRight = { user -> showSuccess(user) }
        )
}

// 📌 PATTERNS TO FOLLOW:
// 1. Use `= either { ... }` for function bodies
// 2. Use `ensure` for simple validations
// 3. Use `.bind()` to unwrap Either values
// 4. Keep either blocks focused and readable
      ```typescript

### Error Hierarchy

      ```typescript
// ✅ Good: Structured error hierarchy
sealed class DomainError {
        data class ValidationError(val field: String, val message: String) : DomainError()
        data class BusinessRuleViolation(val rule: String, val context: String) : DomainError()
        object ScopeNotFound : DomainError()
        data class InvalidParentScope(val parentId: ScopeId, val reason: String) : DomainError()
}

sealed class ApplicationError {
        data class DomainError(val error: com.kamiazya.scopes.domain.DomainError) : ApplicationError()
        data class InfrastructureError(val message: String, val cause: Throwable? = null) : ApplicationError()
        data class AuthorizationError(val operation: String) : ApplicationError()
}

sealed class RepositoryError {
        data class ConnectionError(val cause: Throwable) : RepositoryError()
        data class DataIntegrityError(val message: String) : RepositoryError()
        object NotFound : RepositoryError()
        data class ConflictError(val conflictingId: String) : RepositoryError()
}
      ```typescript

### Error Handling Patterns

      ```typescript
// ✅ Good: Railway-oriented programming with Arrow Either
suspend fun processScopeCreation(request: CreateScopeRequest): Either<ApplicationError, ScopeCreated> = either {
        val validRequest = validateRequest(request)
            .mapLeft { ApplicationError.DomainError(it) }
            .bind()

        checkParentExists(validRequest.parentId).bind()

        val scope = createScopeEntity(request).bind()
        val savedScope = saveScope(scope).bind()

        ScopeCreated(savedScope)
}

// ✅ Good: Alternative with explicit steps
suspend fun processScopeCreation(request: CreateScopeRequest): Either<ApplicationError, ScopeCreated> =
        validateRequest(request)
            .flatMap { validRequest -> checkParentExists(validRequest.parentId) }
            .flatMap { createScopeEntity(request) }
            .flatMap { scope -> saveScope(scope) }
            .map { savedScope -> ScopeCreated(savedScope) }
            .mapLeft { domainError -> ApplicationError.DomainError(domainError) }

// ❌ Bad: Exception-based error handling
suspend fun processScopeCreation(request: CreateScopeRequest): ScopeCreated {
        if (request.title.isBlank()) {
            throw ValidationException("Title cannot be blank")
        }

        val scope = createScopeEntity(request)
        return try {
            val savedScope = saveScope(scope)
            ScopeCreated(savedScope)
        } catch (e: RepositoryException) {
            throw ApplicationException("Failed to save scope", e)
        }
}
      ```typescript

## Testing Standards

### Test Structure with Kotest

      ```typescript
// ✅ Good: Kotest FunSpec with descriptive test names
class CreateScopeUseCaseTest : FunSpec({
        context("CreateScopeUseCase") {
            test("should create scope successfully with valid request") {
                // Given
                val request = CreateScopeRequest(
                    title = "Valid Scope Title",
                    description = "Valid description",
                    parentId = null
                )

                val mockRepository = mockk<ScopeRepository>()
                val mockPublisher = mockk<DomainEventPublisher>()

                every { mockRepository.save(any()) } returns Either.Right(
                    Scope(
                        id = ScopeId.generate(),
                        title = request.title,
                        description = request.description,
                        parentId = null,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                )

                val useCase = CreateScopeUseCase(mockRepository, mockPublisher)

                // When
                val result = runBlocking { useCase.execute(request) }

                // Then
                result.isRight() shouldBe true
                verify { mockRepository.save(any()) }
            }

            test("should fail when title is blank") {
                // Given
                val request = CreateScopeRequest(title = "", description = null, parentId = null)
                val useCase = CreateScopeUseCase(mockk(), mockk())

                // When
                val result = runBlocking { useCase.execute(request) }

                // Then
                result.isLeft() shouldBe true
                result.fold(
                    ifLeft = { error ->
                        error shouldBe instanceOf<ApplicationError.DomainError>()
                    },
                    ifRight = { fail("Expected Left but got Right") }
                )
            }
        }
})
      ```typescript

### Property-Based Testing

      ```typescript
// ✅ Good: Property-based testing with Kotest
class ScopeIdTest : FunSpec({
        test("generated ScopeIds should always be unique") {
            checkAll(iterations = 1000) {
                val id1 = ScopeId.generate()
                val id2 = ScopeId.generate()
                id1 shouldNotBe id2
            }
        }

        test("ScopeId.from should round-trip correctly") {
            checkAll<String> { validUlid ->
                assume(Ulid.isValid(validUlid))

                val scopeId = ScopeId.from(validUlid)
                val stringRepresentation = scopeId.toString()
                val reconstructed = ScopeId.from(stringRepresentation)

                reconstructed shouldBe scopeId
            }
        }
})
      ```typescript

### Mock Usage Guidelines

      ```typescript
// ✅ Good: Focused mocking with clear behavior
class ScopeServiceTest : FunSpec({
        test("should handle repository failure gracefully") {
            val mockRepository = mockk<ScopeRepository>()
            val service = ScopeService(mockRepository)

            // Mock specific behavior
            every { mockRepository.save(any()) } returns Either.Left(
                RepositoryError.ConnectionError(Exception("Database unavailable"))
            )

            val result = runBlocking {
                service.createScope(CreateScopeRequest("Test", null, null))
            }

            result.isLeft() shouldBe true
            verify(exactly = 1) { mockRepository.save(any()) }
        }
})

// ❌ Bad: Over-mocking and unclear test intentions
class ScopeServiceTest : FunSpec({
        test("should work") {
            val mockRepo = mockk<ScopeRepository>()
            val mockValidator = mockk<ScopeValidator>()
            val mockPublisher = mockk<EventPublisher>()
            val mockLogger = mockk<Logger>()

            every { mockRepo.save(any()) } returns mockk()
            every { mockValidator.validate(any()) } returns true
            every { mockPublisher.publish(any()) } just Runs
            every { mockLogger.info(any()) } just Runs

            // Test becomes unclear due to excessive mocking
        }
})
      ```typescript

## Documentation Standards

### KDoc Comments

      ```typescript
// ✅ Good: Comprehensive KDoc with examples
/**
 * Creates a new scope with the provided information.
 *
 * This function validates the input, creates a new scope entity with a generated ULID,
 * and returns a Result indicating success or failure.
 *
 * @param title The scope title, must be non-blank and <= 200 characters
 * @param description Optional description for the scope
 * @param parentId Optional parent scope identifier for hierarchical organization
 * @return Result containing the created Scope on success, or DomainError on failure
 *
 * @throws IllegalArgumentException if title is blank (in validation layer)
 *
 * @sample
 * ```kotlin
 * val result = createScope(
 *     title = "Project Planning",
 *     description = "Planning phase for new project",
 *     parentId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FAV")
 * )
 *
 * when (result) {
 *     is Result.Success -> println("Created scope: ${result.value.id}")
 *     is Result.Failure -> println("Error: ${result.error}")
 * }
 * ```
 */
fun createScope(
        title: String,
        description: String?,
        parentId: ScopeId?
): Result<Scope, DomainError>
      ```typescript

### Code Comments

      ```typescript
// ✅ Good: Comments explain "why", not "what"
fun calculateScopeDepth(scope: Scope, allScopes: List<Scope>): Int {
        // Use tail recursion to prevent stack overflow for deeply nested hierarchies
        tailrec fun calculateDepthRecursive(currentId: ScopeId?, depth: Int): Int =
            when (currentId) {
                null -> depth
                else -> {
                    val parent = allScopes.find { it.id == currentId }?.parentId
                    calculateDepthRecursive(parent, depth + 1)
                }
            }

        return calculateDepthRecursive(scope.parentId, 0)
}

// ❌ Bad: Comments that restate the code
fun calculateScopeDepth(scope: Scope, allScopes: List<Scope>): Int {
        // Declare a recursive function
        tailrec fun calculateDepthRecursive(currentId: ScopeId?, depth: Int): Int =
            // Check if currentId is null
            when (currentId) {
                // If null, return depth
                null -> depth
                // Otherwise, find parent and recurse
                else -> {
                    val parent = allScopes.find { it.id == currentId }?.parentId
                    calculateDepthRecursive(parent, depth + 1)
                }
            }

        // Call the recursive function with scope's parent ID and depth 0
        return calculateDepthRecursive(scope.parentId, 0)
}
      ```typescript

## Tool Configuration

### Quality Tools Integration

```mermaid
flowchart LR
        subgraph "Development Workflow"
            A[Code Writing] --> B[Pre-commit Hooks]
            B --> C[CI/CD Pipeline]
            C --> D[Deployment]

            subgraph "Pre-commit Tools"
                E[EditorConfig]
                F[ktlint]
                G[Detekt]
                H[Prettier]
            end

            subgraph "CI Tools"
                I[Build]
                J[Test]
                K[Quality Gate]
            end

            B --> E
            B --> F
            B --> G
            B --> H

            C --> I
            C --> J
            C --> K
        end

        classDef development fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
        classDef precommit fill:#f1f8e9,stroke:#689f38,stroke-width:2px
        classDef ci fill:#fff3e0,stroke:#ff9800,stroke-width:2px

        class A,B,C,D development
        class E,F,G,H precommit
        class I,J,K ci
      ```typescript

### EditorConfig

Our `.editorconfig` enforces:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.md]
trim_trailing_whitespace = false

[*.yml,*.yaml]
indent_size = 2
      ```typescript

### Detekt Rules

Key enforced rules from `detekt.yml`:

- **MaxLineLength**: 120 characters
- **FunctionMaxLength**: 60 lines
- **ComplexInterface**: Max 10 members
- **CyclomaticComplexMethod**: Max complexity 15
- **ReturnCount**: Max 2 returns per function
- **TooManyFunctions**: Max 11 functions per class

### Ktlint Configuration

In `build.gradle.kts`:

      ```typescript
ktlint {
        version.set("1.5.0")
        outputToConsole.set(true)
        coloredOutput.set(true)
        verbose.set(true)
        android.set(false)
        ignoreFailures.set(false)

        filter {
            exclude("**/generated/**")
            include("**/src/**/*.kt")
        }
}
      ```typescript

## Enforcement

### Pre-commit Hooks

All standards are enforced through lefthook:

```yaml
# lefthook.yml
pre-commit:
    commands:
        format-markdown:
          glob: "*.md"
          run: docker run --rm -v "${PWD}:/work" tmknom/prettier --write {staged_files}

        check-editorconfig:
          run: docker run --rm -v "${PWD}:/check" mstruebing/editorconfig-checker

        ktlint:
          glob: "*.kt"
          run: ./gradlew ktlintCheck

        detekt:
          run: ./gradlew detekt

        test:
          run: ./gradlew test
      ```typescript

### CI/CD Integration

GitHub Actions verify all standards:

```yaml
- name: Run code quality checks
    run: |
        ./gradlew ktlintCheck
        ./gradlew detekt
        ./gradlew test
      ```typescript

### IDE Integration

Recommended IntelliJ IDEA plugins:
- **Detekt**: Real-time static analysis
- **EditorConfig**: Automatic formatting
- **Kotest**: Enhanced test support

## Arrow Either Best Practices

### Quick Reference

      ```typescript
// 1. ALWAYS prefer either DSL for new functions
fun doSomething(input: String): Either<Error, Result> = either {
        // validations, operations, transformations
}

// 2. Use ensure for validations
ensure(condition) { Error.Reason }

// 3. Use bind() to unwrap Either values
val result = someOperation().bind()

// 4. Use raise() for early returns with errors
if (badCondition) raise(Error.BadCondition)

// 5. Keep either blocks simple and readable
fun process(data: Data): Either<Error, ProcessedData> = either {
        val validated = validate(data).bind()
        val transformed = transform(validated).bind()
        save(transformed).bind()
}
      ```typescript

### Common Patterns

      ```typescript
// Repository pattern
suspend fun save(entity: Entity): Either<RepositoryError, Entity> = either {
        try {
            database.save(entity)
        } catch (e: Exception) {
            raise(RepositoryError.SaveFailed(e))
        }
}

// Validation pattern
fun validate(input: Input): Either<ValidationError, ValidInput> = either {
        ensure(input.name.isNotBlank()) { ValidationError.EmptyName }
        ensure(input.age > 0) { ValidationError.InvalidAge }
        ValidInput(input.name.trim(), input.age)
}

// Use case pattern
suspend fun execute(request: Request): Either<UseCaseError, Response> = either {
        val validated = validate(request).bind()
        val entity = createEntity(validated).bind()
        val saved = repository.save(entity)
            .mapLeft { UseCaseError.RepositoryError(it) }
            .bind()
        Response(saved)
}
      ```typescript

## Summary

These coding standards ensure:

- **Consistency**: Uniform code style across the project
- **Simplicity**: Prefer `either { ... }` DSL for cleaner code
- **Quality**: High-quality, maintainable code
- **Functional Purity**: Embrace functional programming benefits
- **Architecture Compliance**: Adherence to Clean Architecture principles
- **Error Safety**: Explicit error handling with Arrow Either
- **Testability**: Comprehensive testing with clear standards

All standards are automatically enforced through tools and should never be bypassed without explicit justification documented in code reviews.
