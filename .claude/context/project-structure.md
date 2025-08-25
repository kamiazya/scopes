---
created: 2025-08-25T14:33:19Z
last_updated: 2025-08-25T14:33:19Z
version: 1.0
author: Claude Code PM System
---

# Project Structure

## Directory Organization

### Root Structure
```
scopes/
├── .claude/           # Claude Code configuration and context
├── .github/           # GitHub workflows and configuration
├── .gradle/           # Gradle build cache
├── .idea/             # IntelliJ IDEA configuration
├── .kotlin/           # Kotlin compiler cache
├── .serena/           # Serena AI tool configuration
├── .vscode/           # VS Code configuration
├── apps/              # Application modules
├── build/             # Build output directory
├── contexts/          # Domain bounded contexts
├── contracts/         # Shared contracts and interfaces
├── docs/              # Project documentation
├── gradle/            # Gradle wrapper and configuration
├── install/           # Installation scripts and resources
├── interfaces/        # External interfaces (CLI, API, etc.)
├── platform/          # Platform-specific implementations
├── quality/           # Quality assurance tools and tests
├── scripts/           # Development and utility scripts
└── tmp/               # Temporary files
```

### Key Directories

#### `/contexts/` - Bounded Contexts
Domain-driven design bounded contexts, each representing a cohesive business capability:
- Each context is self-contained with its own domain, application, and infrastructure layers
- Follows Clean Architecture principles

#### `/apps/` - Applications
Executable applications that compose multiple contexts:
- CLI applications
- Server applications
- Desktop/mobile applications

#### `/platform/` - Platform Layer
Cross-cutting concerns and shared infrastructure:
- Database adapters
- External service integrations
- Common utilities

#### `/interfaces/` - External Interfaces
User and system interfaces:
- CLI commands and parsers
- REST/GraphQL APIs
- Event streams

#### `/contracts/` - Shared Contracts
Interfaces and data structures shared between contexts:
- Domain events
- Integration contracts
- Common value objects

#### `/docs/` - Documentation
Comprehensive project documentation following Diátaxis framework:
- `/docs/tutorials/` - Learning-oriented guides
- `/docs/guides/` - Task-oriented how-tos
- `/docs/reference/` - Technical specifications
- `/docs/explanation/` - Conceptual explanations
- `/docs/explanation/adr/` - Architecture Decision Records

### File Naming Patterns

#### Source Files
- **Kotlin Classes**: `PascalCase.kt` (e.g., `ScopeEntity.kt`)
- **Test Files**: `*Test.kt` or `*Spec.kt` (e.g., `ScopeEntityTest.kt`)
- **Use Cases**: `*UseCase.kt` (e.g., `CreateScopeUseCase.kt`)
- **Value Objects**: `*VO.kt` or descriptive names (e.g., `ScopeId.kt`)

#### Configuration Files
- **Gradle**: `build.gradle.kts`, `settings.gradle.kts`
- **Properties**: `gradle.properties`, `*.properties`
- **YAML**: `*.yml` (e.g., `lefthook.yml`)

#### Documentation
- **Markdown**: `*.md` with kebab-case (e.g., `domain-overview.md`)
- **ADRs**: `NNNN-decision-title.md` (e.g., `0007-domain-driven-design-adoption.md`)

### Module Organization

Each bounded context follows this structure:
```
contexts/[context-name]/
├── domain/            # Pure domain logic
│   ├── src/
│   │   ├── main/kotlin/
│   │   └── test/kotlin/
│   └── build.gradle.kts
├── application/       # Use cases and application services
│   ├── src/
│   │   ├── main/kotlin/
│   │   └── test/kotlin/
│   └── build.gradle.kts
└── infrastructure/    # External adapters and implementations
    ├── src/
    │   ├── main/kotlin/
    │   └── test/kotlin/
    └── build.gradle.kts
```

### Build Configuration
- **Root Build**: `build.gradle.kts` - Main project configuration
- **Settings**: `settings.gradle.kts` - Multi-module project structure
- **Module Builds**: Each module has its own `build.gradle.kts`

### Quality Tools
- **Detekt**: `detekt.yml` - Kotlin static analysis
- **Konsist**: Architecture tests in test sources
- **Lefthook**: `lefthook.yml` - Git hooks configuration
