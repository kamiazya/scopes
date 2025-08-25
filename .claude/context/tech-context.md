---
created: 2025-08-25T14:33:19Z
last_updated: 2025-08-25T14:33:19Z
version: 1.0
author: Claude Code PM System
---

# Technology Context

## Core Technologies

### Programming Language
- **Kotlin** (JVM)
  - Target JVM 17+
  - Kotlin DSL for Gradle builds
  - Coroutines for asynchronous programming
  - Functional programming patterns

### Build System
- **Gradle** 8.x with Kotlin DSL
  - Multi-module project structure
  - Convention plugins for consistent configuration
  - Build cache enabled
  - Gradle Build Scan® integration

### Architecture Frameworks
- **Clean Architecture** - Separation of concerns
- **Domain-Driven Design (DDD)** - Business logic modeling
- **Hexagonal Architecture** - Ports and adapters pattern

## Dependencies

### Testing Frameworks
- **JUnit 5** - Unit testing framework
- **Kotest** - Property-based testing and assertions
- **Konsist** - Architecture testing and validation
- **MockK** - Mocking framework for Kotlin

### Code Quality Tools
- **Detekt** - Static code analysis
- **Ktlint** - Kotlin code formatting
- **SonarQube** - Code quality metrics
- **Kover** - Code coverage reporting

### Build Tools
- **GraalVM** - Native image compilation
- **Shadow JAR** - Fat JAR packaging
- **Gradle Versions Plugin** - Dependency updates
- **OWASP Dependency Check** - Security vulnerability scanning

### Infrastructure Libraries
- **Kotlin Coroutines** - Asynchronous programming
- **Kotlin Serialization** - JSON/Protocol Buffer support
- **Arrow** - Functional programming utilities
- **Clikt** - CLI framework

### External Integrations
- **MCP (Model Context Protocol)** - AI assistant integration
- **GitHub API** - Repository integration
- **ULID** - Unique identifier generation

## Development Tools

### Version Control
- **Git** - Source control
- **GitHub** - Repository hosting
- **GitHub Actions** - CI/CD pipelines

### IDE Support
- **IntelliJ IDEA** - Primary IDE (`.idea/` configuration)
- **VS Code** - Alternative editor (`.vscode/` configuration)

### Git Hooks
- **Lefthook** - Pre-commit and pre-push hooks
  - Code formatting checks
  - Test execution
  - Build validation

### AI Development Tools
- **Claude Code** - AI pair programming
- **Serena** - Code analysis and refactoring
- **MCP Servers** - Context and documentation access

## Dependency Management

### Version Catalog
Centralized dependency management through Gradle version catalogs:
- Libraries defined in `gradle/libs.versions.toml`
- Consistent versions across modules
- Easy dependency updates

### Security Scanning
- OWASP Dependency Check for vulnerability detection
- SBOM (Software Bill of Materials) generation
- Regular dependency updates via Dependabot

## Platform Requirements

### Runtime
- **JVM 17+** - Minimum Java version
- **GraalVM** - For native image builds (optional)

### Operating Systems
- **Linux** - Primary deployment target
- **macOS** - Development and deployment
- **Windows** - Full support planned

### Architecture Support
- **x86_64** - Primary architecture
- **ARM64** - Full support (Apple Silicon, AWS Graviton)

## Development Practices

### Testing Strategy
- **Unit Tests** - Pure domain logic testing
- **Integration Tests** - Module boundary testing
- **Property-Based Tests** - Value objects and invariants
- **Architecture Tests** - Clean Architecture validation

### Code Style
- Kotlin coding conventions
- Functional programming preferences
- Immutable data structures
- Explicit error handling with Result types

### Documentation
- **KDoc** - Code documentation
- **Markdown** - Project documentation
- **PlantUML** - Architecture diagrams
- **ADRs** - Decision documentation
