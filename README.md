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
├── apps/                      # Application layer
│   ├── api/                   # REST API server
│   ├── cli/                   # Command-line interface
│   └── daemon/                # Background service
├── boot/                      # Application bootstrapping
│   ├── cli-launcher/          # CLI startup
│   └── daemon-launcher/       # Daemon startup
├── contexts/                  # Bounded contexts (DDD)
│   └── scope-management/      # Unified scope management context
│       ├── domain/            # Core business logic
│       ├── application/       # Use cases and orchestration
│       └── infrastructure/    # External integrations
├── docs/                      # Documentation (Diátaxis framework)
│   ├── explanation/           # Conceptual documentation
│   │   └── adr/              # Architecture Decision Records
│   ├── guides/               # How-to guides
│   └── reference/            # API and technical reference
└── libs/                     # Shared libraries
    ├── common/               # Common utilities
    └── test-utils/           # Testing utilities
```

### Bounded Context

The architecture has been refactored to consolidate all scope-related functionality into a single **scope-management** context, following the principle of high cohesion. This unified context manages:

- **Scope Aggregate**: Core entity representing projects, tasks, and all work units
- **Aspect System**: Flexible key-value metadata for multi-dimensional classification
- **Hierarchy Management**: Parent-child relationships with configurable depth
- **Cross-cutting Concerns**: Comments, attachments, relations, and other features

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
./gradlew :boot:cli-launcher:run

# API Server
./gradlew :apps:api:run

# Daemon
./gradlew :boot:daemon-launcher:run
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