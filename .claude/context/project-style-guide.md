---
created: 2025-08-25T14:33:19Z
last_updated: 2025-08-25T14:33:19Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Coding Standards

### Kotlin Code Style

#### Naming Conventions

**Classes and Interfaces**
```kotlin
// Classes: PascalCase, descriptive nouns
class ScopeEntity
class CreateScopeUseCase

// Interfaces: PascalCase, descriptive
interface ScopeRepository
interface UseCase<Request, Response>

// Value Objects: Descriptive name or suffix with VO
class ScopeId  // Clear from context
class TitleVO  // When clarity needed
```

**Functions and Properties**
```kotlin
// Functions: camelCase, verb phrases
fun createScope(title: Title): Scope
fun validateTitle(value: String): Result<Title>

// Properties: camelCase, noun phrases
val scopeId: ScopeId
val createdAt: Instant
val isCompleted: Boolean  // Boolean properties use is/has prefix
```

**Constants and Enums**
```kotlin
// Constants: UPPER_SNAKE_CASE
const val MAX_TITLE_LENGTH = 200
const val DEFAULT_PAGE_SIZE = 20

// Enum values: UPPER_SNAKE_CASE
enum class ScopeStatus {
    PLANNING,
    IN_PROGRESS,
    COMPLETED
}
```

**Packages**
```kotlin
// Package names: lowercase, no underscores
package io.github.kamiazya.scopes.domain.model
package io.github.kamiazya.scopes.application.usecase
```

#### Code Organization

**File Structure**
```kotlin
// One public class per file
// File name matches class name
// Related private classes in same file

// ScopeEntity.kt
class ScopeEntity(
    val id: ScopeId,
    val title: Title,
    val description: Description
) {
    // Public members first
    fun addChild(child: ScopeEntity): ScopeEntity
    
    // Private members last
    private fun validateChild(child: ScopeEntity)
}

// Private helper class in same file
private class ScopeValidator
```

**Import Organization**
```kotlin
// Order: stdlib, third-party, project
import kotlin.collections.List
import kotlin.time.Duration

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

import io.github.kamiazya.scopes.domain.model.Scope
import io.github.kamiazya.scopes.domain.error.DomainError
```

### Architecture Conventions

#### Clean Architecture Layers

**Domain Layer**
- Pure Kotlin, no framework dependencies
- Immutable entities and value objects
- Rich domain model with business logic
- Domain errors as sealed classes

**Application Layer**
- Use cases implement business workflows
- Orchestrates domain logic
- Defines port interfaces
- Transaction boundaries

**Infrastructure Layer**
- Implements port interfaces
- Framework-specific code
- External service adapters
- Database implementations

#### Domain-Driven Design

**Entities**
```kotlin
// Entities have identity and lifecycle
class ScopeEntity(
    val id: ScopeId,  // Identity
    val title: Title,
    val status: ScopeStatus
) {
    // Business logic methods
    fun complete(): Result<ScopeEntity, DomainError>
    fun addTag(tag: Tag): Result<ScopeEntity, DomainError>
}
```

**Value Objects**
```kotlin
// Value objects are immutable and compared by value
@JvmInline
value class ScopeId(val value: String) {
    init {
        require(value.isNotBlank()) { "ScopeId cannot be blank" }
    }
}

// Multi-property value object
data class Title(val value: String) {
    init {
        require(value.length <= MAX_TITLE_LENGTH) {
            "Title cannot exceed $MAX_TITLE_LENGTH characters"
        }
    }
}
```

**Use Cases**
```kotlin
// Use cases follow a standard pattern
class CreateScopeUseCase(
    private val scopeRepository: ScopeRepository,
    private val eventBus: EventBus
) : UseCase<CreateScopeRequest, CreateScopeResponse> {
    
    override suspend fun execute(
        request: CreateScopeRequest
    ): Result<CreateScopeResponse, DomainError> {
        // Validation
        // Business logic
        // Persistence
        // Event publishing
    }
}
```

### Testing Conventions

#### Test Organization
```kotlin
// Test class naming: ClassNameTest or ClassNameSpec
class ScopeEntityTest {
    @Nested
    inner class `when creating a scope` {
        @Test
        fun `should have the provided title`() {
            // Given
            val title = Title("My Scope")
            
            // When
            val scope = ScopeEntity(title = title)
            
            // Then
            scope.title shouldBe title
        }
    }
}
```

#### Property-Based Testing
```kotlin
// Use Kotest property testing for value objects
class TitleTest : StringSpec({
    "Title should accept any non-empty string up to max length" {
        checkAll(Arb.string(1..MAX_TITLE_LENGTH)) { value ->
            val title = Title(value)
            title.value shouldBe value
        }
    }
})
```

### Documentation Standards

#### KDoc Comments
```kotlin
/**
 * Represents a scope in the task management hierarchy.
 * 
 * A scope can contain child scopes, forming a tree structure
 * with unlimited depth.
 *
 * @property id Unique identifier for this scope
 * @property title Human-readable title
 * @property children Child scopes in the hierarchy
 */
class ScopeEntity(
    val id: ScopeId,
    val title: Title,
    val children: List<ScopeEntity> = emptyList()
) {
    /**
     * Adds a child scope to this scope.
     *
     * @param child The scope to add as a child
     * @return A new scope instance with the child added
     * @throws DomainError.ValidationError if child would create a cycle
     */
    fun addChild(child: ScopeEntity): Result<ScopeEntity, DomainError>
}
```

#### Inline Comments
```kotlin
// Use comments sparingly for non-obvious logic
fun calculateDepth(): Int {
    // Recursive depth calculation to handle unlimited hierarchy
    return 1 + (children.maxOfOrNull { it.calculateDepth() } ?: 0)
}

// TODO: Implement cycle detection
// FIXME: Handle edge case for orphaned scopes
// NOTE: This is optimized for read-heavy workloads
```

### File Structure Patterns

#### Module Structure
```
contexts/scope-management/
├── domain/
│   └── src/
│       ├── main/kotlin/
│       │   ├── model/       # Entities and VOs
│       │   ├── error/       # Domain errors
│       │   ├── event/       # Domain events
│       │   └── service/     # Domain services
│       └── test/kotlin/
├── application/
│   └── src/
│       ├── main/kotlin/
│       │   ├── usecase/     # Use case implementations
│       │   ├── port/        # Port interfaces
│       │   └── dto/         # Data transfer objects
│       └── test/kotlin/
└── infrastructure/
    └── src/
        ├── main/kotlin/
        │   ├── adapter/     # External adapters
        │   ├── repository/  # Repository implementations
        │   └── config/      # Configuration
        └── test/kotlin/
```

### Git Commit Style

#### Commit Message Format
```
type(scope): subject

body

footer
```

#### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, missing semicolons, etc)
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding missing tests
- `chore`: Changes to build process or auxiliary tools

#### Examples
```
feat(scope): implement recursive scope hierarchy

- Add parent-child relationship to ScopeEntity
- Implement unlimited depth support per ADR-0011
- Add cycle detection for safety

Closes #123
```

### Error Handling Style

#### Domain Errors
```kotlin
sealed interface DomainError {
    data class ValidationError(
        val field: String,
        val message: String
    ) : DomainError
    
    data class NotFoundError(
        val entity: String,
        val id: String
    ) : DomainError
}
```

#### Result Type Usage
```kotlin
// Use Result<T, E> for expected errors
fun createScope(title: String): Result<Scope, DomainError> =
    Title.create(title)
        .map { validTitle -> Scope(title = validTitle) }

// Use exceptions only for programming errors
fun getScope(id: String): Scope {
    requireNotNull(id) { "ID cannot be null" }  // Programming error
    return repository.find(id)
        ?: throw IllegalStateException("Scope must exist")  // Invariant violation
}
```
