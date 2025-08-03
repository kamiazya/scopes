# Development Guidelines

This guide provides comprehensive guidelines for developing the Scopes project, based on our architectural decisions and coding standards.


## Architecture Overview

Scopes follows **Clean Architecture** principles with **Domain-Driven Design (DDD)** and **Functional Programming** paradigms. Key architectural decisions are documented in our [ADRs](../explanation/adr/).

### Core Architectural Principles

1. **Local-First Architecture** ([ADR-0001](../explanation/adr/0001-local-first-architecture.md))
2. **AI-Driven Development** ([ADR-0002](../explanation/adr/0002-ai-driven-development-architecture.md))
3. **Clean Architecture** ([ADR-0008](../explanation/adr/0008-clean-architecture-adoption.md))
4. **Domain-Driven Design** ([ADR-0007](../explanation/adr/0007-domain-driven-design-adoption.md))
5. **Functional DDD** ([ADR-0011](../explanation/adr/0011-functional-ddd-adoption.md))

## Clean Architecture Guidelines

### Module Dependencies

```mermaid
flowchart TD
        CLI[presentation-cli]
        APP[application]
        DOM[domain]
        INF[infrastructure]
        
        CLI --> APP
        CLI --> INF
        APP --> DOM
        INF --> DOM
        
        classDef domain fill:#e8f5e8,stroke:#4caf50,stroke-width:3px
        classDef application fill:#e3f2fd,stroke:#2196f3,stroke-width:3px
        classDef infrastructure fill:#fff3e0,stroke:#ff9800,stroke-width:3px
        classDef presentation fill:#f3e5f5,stroke:#9c27b0,stroke-width:3px
        
        class DOM domain
        class APP application
        class INF infrastructure
        class CLI presentation
      ```typescript

### Dependency Rules

- **Domain Layer**: No dependencies on other layers
- **Application Layer**: Depends only on Domain
- **Infrastructure Layer**: Depends only on Domain
- **Presentation Layer**: Depends on Application and Infrastructure

### Layer Responsibilities

#### Domain Layer (`:domain`)
- Entities, Value Objects, Domain Services
- Repository interfaces
- Domain events and aggregates
- Business rules and invariants

#### Application Layer (`:application`)
- Use cases and application services
- Command/Query handlers
- Application-specific business logic
- Orchestration of domain objects

#### Infrastructure Layer (`:infrastructure`)
- Repository implementations
- External service integrations
- Data persistence
- Technical concerns

#### Presentation Layer (`:presentation-cli`)
- User interface (CLI commands)
- Input validation and formatting
- Response mapping
- Error presentation

## Functional DDD Implementation

Following [ADR-0011](../explanation/adr/0011-functional-ddd-adoption.md), we implement Domain-Driven Design using functional programming principles.

### Core Principles

```mermaid
flowchart LR
        subgraph "Functional DDD Principles"
            A[Immutable Domain Objects]
            B[Pure Functions for Business Logic]
            C[Explicit Error Handling with Result Types]
            D[Function Composition]
            E[Domain Events as Data]
            
            A --> B
            B --> C
            C --> D
            D --> E
        end
        
        classDef principle fill:#f1f8e9,stroke:#689f38,stroke-width:2px
        class A,B,C,D,E principle
      ```typescript

1. **Immutable Domain Objects**
2. **Pure Functions for Business Logic**
3. **Explicit Error Handling with Result Types**
4. **Function Composition**
5. **Domain Events as Data**

### Implementation Patterns

#### 1. Immutable Entities

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
        // Pure functions for business logic
        fun updateTitle(newTitle: String): Scope =
            copy(title = newTitle, updatedAt = Clock.System.now())
        
        fun addChild(childId: ScopeId): ScopeCreated =
            ScopeCreated(parentId = id, childId = childId)
}

// ❌ Bad: Mutable entity
class Scope {
        var title: String = ""
        fun setTitle(newTitle: String) {
            this.title = newTitle
        }
}
      ```typescript

#### 2. Value Objects with ULID

      ```typescript
// ✅ Good: Immutable value object with ULID
@JvmInline
value class ScopeId private constructor(private val value: String) {
        companion object {
            fun generate(): ScopeId = ScopeId(Ulid.fast().toString())
            fun from(value: String): ScopeId = ScopeId(value)
        }
        
        override fun toString(): String = value
}
      ```typescript

#### 3. Result Types for Error Handling

      ```typescript
// Define Result type
sealed class Result<out T, out E> {
        data class Success<T>(val value: T) : Result<T, Nothing>()
        data class Failure<E>(val error: E) : Result<Nothing, E>()
}

// ✅ Good: Use Result types
interface ScopeRepository {
        suspend fun findById(id: ScopeId): Result<Scope?, RepositoryError>
        suspend fun save(scope: Scope): Result<Scope, RepositoryError>
}

// ❌ Bad: Exception-based approach
interface ScopeRepository {
        @Throws(RepositoryException::class)
        suspend fun findById(id: ScopeId): Scope?
}
      ```typescript

#### 4. Pure Domain Services

      ```typescript
// ✅ Good: Pure function domain service
object ScopeValidationService {
        fun validateTitle(title: String): Result<String, ValidationError> =
            when {
                title.isBlank() -> Result.Failure(ValidationError.EmptyTitle)
                title.length > 200 -> Result.Failure(ValidationError.TitleTooLong)
                else -> Result.Success(title.trim())
            }
}

// ❌ Bad: Service with side effects
class ScopeValidationService {
        private val logger = LoggerFactory.getLogger(this::class.java)
        
        fun validateTitle(title: String): Boolean {
            logger.info("Validating title: $title") // Side effect
            return title.isNotBlank()
        }
}
      ```typescript

## Coding Standards

### Kotlin Style Guidelines

#### 1. Naming Conventions

      ```typescript
// ✅ Good: Clear, descriptive names
class CreateScopeUseCase
data class ScopeCreationRequest
sealed class DomainError

// ❌ Bad: Abbreviations and unclear names
class CSUseCase
data class ScopeReq
sealed class Err
      ```typescript

#### 2. Function Composition

      ```typescript
// ✅ Good: Function composition with Result types
fun createScope(request: CreateScopeRequest): Result<Scope, DomainError> =
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
      ```typescript

#### 3. Sealed Classes for Domain Modeling

      ```typescript
// ✅ Good: Sealed classes for domain states
sealed class ScopeCommand {
        data class Create(val title: String, val description: String?) : ScopeCommand()
        data class Update(val id: ScopeId, val title: String) : ScopeCommand()
        data class Delete(val id: ScopeId) : ScopeCommand()
}

sealed class DomainError {
        object ScopeNotFound : DomainError()
        data class ValidationFailed(val message: String) : DomainError()
        data class RepositoryError(val cause: Throwable) : DomainError()
}
      ```typescript

### Code Organization

#### 1. Package Structure

```mermaid
flowchart TD
        subgraph "Project Structure"
            ROOT[scopes/]
            
            subgraph DOCS["docs/"]
                DOCS_EXP[explanation/<br/>ADRs, Architecture]
                DOCS_GUIDES[guides/<br/>Implementation guides]
                DOCS_REF[reference/<br/>CLI reference]
            end
            
            subgraph DOM["domain/"]
                DOM_ENT[entity/<br/>Scope.kt, ScopeId.kt]
                DOM_REP[repository/<br/>ScopeRepository.kt]
            end
            
            subgraph APP["application/"]
                APP_USE[usecase/<br/>CreateScopeUseCase.kt]
                APP_SRV[service/<br/>ScopeService.kt]
            end
            
            subgraph INF["infrastructure/"]
                INF_REP[repository/<br/>InMemoryScopeRepository.kt]
            end
            
            subgraph CLI["presentation-cli/"]
                CLI_MAIN[Main.kt, ScopesCommand.kt]
                CLI_CMD[commands/<br/>CreateCommand.kt, ListCommand.kt]
            end
            
            subgraph CONFIG["Configuration"]
                GRADLE[build.gradle.kts<br/>libs.versions.toml]
                QUALITY[detekt.yml<br/>lefthook.yml]
                SCRIPTS[scripts/<br/>check-*.sh]
            end
            
            ROOT --> DOCS
            ROOT --> DOM
            ROOT --> APP
            ROOT --> INF
            ROOT --> CLI
            ROOT --> CONFIG
            
            DOCS --> DOCS_EXP
            DOCS --> DOCS_GUIDES
            DOCS --> DOCS_REF
            
            DOM --> DOM_ENT
            DOM --> DOM_REP
            
            APP --> APP_USE
            APP --> APP_SRV
            
            INF --> INF_REP
            
            CLI --> CLI_MAIN
            CLI --> CLI_CMD
            
            CONFIG --> GRADLE
            CONFIG --> QUALITY
            CONFIG --> SCRIPTS
        end
        
        classDef domain fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
        classDef application fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
        classDef infrastructure fill:#fff3e0,stroke:#ff9800,stroke-width:2px
        classDef presentation fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px
        classDef docs fill:#e8eaf6,stroke:#3f51b5,stroke-width:2px
        classDef config fill:#f5f5f5,stroke:#666,stroke-width:2px
        classDef root fill:#fff8e1,stroke:#f57f17,stroke-width:3px
        
        class DOM,DOM_ENT,DOM_REP domain
        class APP,APP_USE,APP_SRV application
        class INF,INF_REP infrastructure
        class CLI,CLI_MAIN,CLI_CMD presentation
        class DOCS,DOCS_EXP,DOCS_GUIDES,DOCS_REF docs
        class CONFIG,GRADLE,QUALITY,SCRIPTS config
        class ROOT root
      ```typescript

#### 2. File Naming

- Use PascalCase for classes: `CreateScopeUseCase.kt`
- One public class per file
- File name matches primary class name

## Module Structure

### Dependencies in `build.gradle.kts`

#### Domain Module
      ```typescript
// domain/build.gradle.kts
dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.datetime)
        implementation(libs.ulid.creator)
        
        testImplementation(libs.bundles.kotest)
}
      ```typescript

#### Application Module
      ```typescript
// application/build.gradle.kts
dependencies {
        implementation(project(":domain"))
        implementation(libs.kotlinx.coroutines.core)
        
        testImplementation(libs.bundles.kotest)
        testImplementation(libs.mockk)
}
      ```typescript

### Version Management

Use `gradle/libs.versions.toml` for centralized version management:

```toml
[versions]
kotlin = "2.2.0"
kotlinx-coroutines = "1.10.2"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }

[bundles]
kotest = ["kotest-runner-junit5", "kotest-assertions-core", "kotest-property"]
      ```typescript

## Error Handling

### Result Type Implementation

```mermaid
flowchart LR
        subgraph "Result Type Pattern"
            A[Input] --> B{Operation}
            B -->|Success| C[Result.Success&lt;T&gt;]
            B -->|Failure| D[Result.Failure&lt;E&gt;]
            
            C --> E[map/flatMap]
            D --> F[Error Propagation]
            
            E --> G[Next Operation]
            F --> G
        end
        
        classDef success fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
        classDef failure fill:#ffebee,stroke:#f44336,stroke-width:2px
        classDef operation fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
        
        class C,E success
        class D,F failure
        class A,B,G operation
      ```typescript

      ```typescript
// Result type extensions for composition
inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
        when (this) {
            is Result.Success -> Result.Success(transform(value))
            is Result.Failure -> this
        }

inline fun <T, E, R> Result<T, E>.flatMap(transform: (T) -> Result<R, E>): Result<R, E> =
        when (this) {
            is Result.Success -> transform(value)
            is Result.Failure -> this
        }
      ```typescript

### Error Hierarchy

```mermaid
classDiagram
        class DomainError {
            <<sealed>>
        }
        class ApplicationError {
            <<sealed>>
        }
        class RepositoryError {
            <<sealed>>
        }
        
        class ScopeNotFound {
            object
        }
        class ValidationFailed {
            +field: String
            +message: String
        }
        class BusinessRuleViolation {
            +rule: String
        }
        
        class DomainErrorWrapper {
            +error: DomainError
        }
        class InfrastructureError {
            +message: String
        }
        
        class ConnectionError {
            +cause: Throwable
        }
        class DataIntegrityError {
            +message: String
        }
        class NotFound {
            object
        }
        
        DomainError <|-- ScopeNotFound
        DomainError <|-- ValidationFailed
        DomainError <|-- BusinessRuleViolation
        
        ApplicationError <|-- DomainErrorWrapper
        ApplicationError <|-- InfrastructureError
        
        RepositoryError <|-- ConnectionError
        RepositoryError <|-- DataIntegrityError
        RepositoryError <|-- NotFound
        
        DomainErrorWrapper --> DomainError : contains
        
        classDef domain fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
        classDef application fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
        classDef infrastructure fill:#fff3e0,stroke:#ff9800,stroke-width:2px
        
        class DomainError domain
        class ScopeNotFound domain
        class ValidationFailed domain
        class BusinessRuleViolation domain
        
        class ApplicationError application
        class DomainErrorWrapper application
        class InfrastructureError application
        
        class RepositoryError infrastructure
        class ConnectionError infrastructure
        class DataIntegrityError infrastructure
        class NotFound infrastructure
      ```typescript

      ```typescript
sealed class DomainError {
        object ScopeNotFound : DomainError()
        data class ValidationFailed(val field: String, val message: String) : DomainError()
        data class BusinessRuleViolation(val rule: String) : DomainError()
}

sealed class ApplicationError {
        data class DomainError(val error: com.kamiazya.scopes.domain.DomainError) : ApplicationError()
        data class InfrastructureError(val message: String) : ApplicationError()
}
      ```typescript

## Testing Guidelines

### Test Structure

Use **Kotest** framework following our architectural decisions:

      ```typescript
class CreateScopeUseCaseTest : FunSpec({
        test("should create scope with valid data") {
            // Given
            val request = CreateScopeRequest(
                title = "Test Scope",
                description = "Test Description"
            )
            val mockRepository = mockk<ScopeRepository>()
            
            every { mockRepository.save(any()) } returns Result.Success(
                // ... scope object
            )
            
            val useCase = CreateScopeUseCase(mockRepository)
            
            // When
            val result = useCase.execute(request)
            
            // Then
            result shouldBe instanceOf<Result.Success<*>>()
        }
})
      ```typescript

### Test Categories

1. **Unit Tests**: Test individual functions and classes
2. **Integration Tests**: Test module interactions
3. **Architecture Tests**: Verify architectural constraints

### Property-Based Testing

      ```typescript
class ScopeIdTest : FunSpec({
        test("ULID generation should be unique") {
            checkAll<String> { _ ->
                val id1 = ScopeId.generate()
                val id2 = ScopeId.generate()
                id1 shouldNotBe id2
            }
        }
})
      ```typescript

## Code Quality Tools

### Quality Pipeline

```mermaid
flowchart LR
        subgraph "Pre-commit Hooks (lefthook)"
            A[Markdown<br/>prettier] --> B[EditorConfig<br/>checker]
            B --> C[Kotlin<br/>ktlint]
            C --> D[Static Analysis<br/>detekt]
            D --> E[Tests<br/>kotest]
        end
        
        subgraph "CI/CD Pipeline"
            F[Build] --> G[Quality Gate]
            G --> H[GraalVM Native<br/>Compilation]
        end
        
        E --> F
        
        classDef quality fill:#f1f8e9,stroke:#689f38,stroke-width:2px
        classDef build fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
        
        class A,B,C,D,E quality
        class F,G,H build
      ```typescript

### Detekt Configuration

Our `detekt.yml` enforces:
- Maximum line length: 120 characters
- Complexity thresholds
- Naming conventions
- Function length limits

### Pre-commit Hooks

Lefthook runs automatically:
- **Markdown formatting**: `prettier`
- **EditorConfig compliance**: `editorconfig-checker`
- **Kotlin linting**: `ktlint`
- **Static analysis**: `detekt`
- **Tests**: `./gradlew test`

### Running Quality Checks

```bash
# Run all quality checks
./gradlew check

# Individual tools
./gradlew ktlint
./gradlew detekt
./gradlew test

# Pre-commit hook simulation
lefthook run pre-commit
      ```typescript

## Development Workflow

### Daily Development

1. **Setup Local Environment**
     ```bash
     git clone <repository>
     cd scopes
     ./gradlew build  # Verify everything works
     ```

2. **Create Feature Branch**
     ```bash
     git checkout -b feature/your-feature-name
     ```

3. **Make Changes**
       - Follow coding standards
       - Write tests for new functionality
       - Update documentation as needed

4. **Auto-format Code** (recommended before commit)
     ```bash
     # Format all files at once
     ./scripts/format-all.sh
     
     # Or format individually:
     ./gradlew ktlintFormat    # Kotlin files
     ```

5. **Test Your Changes**
     ```bash
     ./gradlew test           # Run all tests
     ./gradlew detekt         # Static analysis
     ./gradlew ktlintCheck    # Code formatting check
     ```

6. **Commit Changes**
     ```bash
     git add .
     git commit -m "feat: your descriptive message"
     # Note: Pre-commit hooks will auto-format and stage fixes
     ```

7. **Push and Create PR**
     ```bash
     git push origin feature/your-feature-name
     ```

### Pre-commit Hooks

Pre-commit hooks are configured via `lefthook.yml` and will automatically:
- **Auto-format** Kotlin code with `ktlintFormat`
- **Auto-fix** Markdown issues with `markdownlint --fix`
- **Stage fixed files** automatically
- Run tests and static analysis

To install hooks:
```bash
lefthook install
```

### Manual Formatting Commands

```bash
# Format specific file types
./gradlew ktlintFormat          # Kotlin files
./scripts/format-all.sh         # All supported files

# Check formatting without fixes
./gradlew ktlintCheck           # Kotlin formatting check
./gradlew detekt                # Static analysis

# Run tests
./gradlew test                  # Unit tests
./gradlew build                 # Full build with tests
```

## Best Practices Summary

### Do ✅

- Use immutable data classes for domain objects
- Implement pure functions for business logic
- Use Result types for error handling
- Follow Clean Architecture dependency rules
- Write comprehensive tests with Kotest
- Use ULID for distributed identifiers
- Document architectural decisions in ADRs

### Don't ❌

- Create mutable domain objects
- Use exceptions for business logic errors
- Violate module dependency rules
- Skip tests for critical business logic
- Use UUID instead of ULID
- Implement side effects in domain layer
- Create circular dependencies between modules

## Resources

- [Architecture Decision Records](../explanation/adr/)
- [GraalVM Setup Guide](./graalvm-setup.md)
- [Project Architecture Overview](../explanation/architecture-overview.md)
- [Contributing Guidelines](../../CONTRIBUTING.md)
