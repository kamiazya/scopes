# Scopes

**Scopes** is a next-generation local-first task and project management tool designed for symbiotic collaboration between developers and AI assistants.

## 🎯 Vision

Create a unified, recursive task management system where AI and human developers work together seamlessly, eliminating context loss and maximizing productivity through intelligent workspace management.

## ✨ Core Features

### Unified "Scope" Entity
- **Recursive Structure**: Projects, epics, and tasks are all "Scopes"
- **Unlimited Hierarchy**: No depth restrictions (default 10 levels, configurable)
- **Consistent Operations**: Same features available at every level

### AI-Native Architecture
- **Comment-Based AI Integration**: Asynchronous AI collaboration through comments
- **Workspace + Focus Management**: Automatic context switching based on directory
- **MCP (Model Context Protocol)**: Standard AI integration protocol
- **Natural Language Context**: "this", "that" resolve to focused scope

### Local-First Design
- **Offline-First**: All features work without internet
- **Selective Sync**: Choose what to share with external tools
- **Cross-Platform**: Native support for Windows, macOS, Linux
- **Privacy by Design**: Local-only data stays local

## 🏗️ Architecture

The project follows **Clean Architecture** and **Domain-Driven Design (DDD)** principles with a functional programming approach using Kotlin and Arrow.

### Project Structure

```
scopes/
├── apps/                      # Entry points
│   ├── scopes/                # Main CLI application
│   └── scopesd/               # Background daemon service
├── contexts/                  # Bounded contexts (DDD)
│   ├── scope-management/      # Core scope management context
│   │   ├── domain/            # Business logic and rules
│   │   ├── application/       # Use cases and handlers
│   │   └── infrastructure/    # Technical implementations
│   └── user-preferences/      # User preferences context
│       ├── domain/            # Preference domain model
│       ├── application/       # Preference use cases
│       └── infrastructure/    # Preference storage
├── interfaces/                # Adapters and facades
│   ├── cli/                   # CLI commands and formatters
│   └── shared/                # Shared facades and DI
├── platform/                  # Shared infrastructure
│   ├── commons/               # Core types (ULID, Instant)
│   ├── domain-commons/        # Shared DDD components
│   ├── application-commons/   # Base application types
│   └── observability/         # Logging and monitoring
├── quality/                   # Architecture tests
│   └── konsist/               # Konsist architecture validation
└── docs/                      # Documentation (Diátaxis)
    ├── explanation/           # Conceptual documentation
    │   └── adr/              # Architecture Decision Records
    ├── guides/               # How-to guides
    ├── reference/            # API reference
    └── tutorials/            # Learning guides
```

### Architecture Highlights

#### Clean Architecture Layers
- **Apps**: Entry points (CLI, daemon)
- **Interfaces**: Adapters between external world and application
- **Contexts**: Bounded contexts with domain/application/infrastructure layers
- **Platform**: Shared infrastructure and common components
- **Quality**: Architecture compliance and validation

#### Bounded Context Structure
Each bounded context (e.g., **scope-management**, **user-preferences**) is organized following DDD principles:

- **Domain Layer**: Pure business logic with aggregates, entities, value objects, and domain events
- **Application Layer**: Use cases, command/query handlers, and application services
- **Infrastructure Layer**: Repository implementations, external service adapters

#### Key Design Patterns
- **Event Sourcing**: Aggregate state changes captured as domain events
- **CQRS**: Separate command and query models
- **Repository Pattern**: Abstract persistence behind interfaces
- **Hexagonal Architecture**: Ports and adapters for external dependencies
- **Dependency Injection**: Koin for wiring components

## 🚀 Getting Started

### Prerequisites

- JDK 21 or later
- Gradle 8.x

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
./gradlew konsistTest  # Architecture compliance tests
```

### Run Application

```bash
# CLI
./gradlew :apps-scopes:run

# Daemon
./gradlew :apps-scopesd:run
```

## 📚 Documentation

- [Domain Overview](docs/explanation/domain-overview.md) - Core concepts and domain model
- [Clean Architecture](docs/explanation/clean-architecture.md) - Architectural layers and principles
- [Domain-Driven Design](docs/explanation/domain-driven-design.md) - DDD implementation approach
- [Architecture Decision Records](docs/explanation/adr/) - Key architectural decisions
- [Use Case Style Guide](docs/guides/use-case-style-guide.md) - Implementation patterns

## 🤝 Contributing

This is a greenfield project inheriting the best ideas from Project Manager while introducing the revolutionary unified Scope concept for true AI-developer symbiosis.

### Development Guidelines

1. Follow Clean Architecture and DDD principles
2. Write property-based tests for value objects and events
3. Run `./gradlew konsistTest` to verify architectural compliance
4. Document architectural decisions in ADRs

## 📄 License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

## 🌟 Target Users

- **AI-Driven Developers**: Using AI as primary development partner
- **Tech Leads**: Managing design quality before team presentation
- **OSS Contributors**: Coordinating across multiple projects
- **Multi-Device Developers**: Seamless work across machines
- **International Engineers**: Breaking language barriers with AI
