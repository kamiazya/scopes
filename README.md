[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/kamiazya/scopes)

# Scopes

**Scopes** is a next-generation local-first task and project management tool designed for symbiotic collaboration between developers and AI assistants.

## 🎯 Vision

Create a unified, recursive task management system where AI and human developers work together seamlessly, eliminating context loss and maximizing productivity through intelligent workspace management.

## ✨ Core Features

### Unified "Scope" Entity
- **Recursive Structure**: Projects, epics, and tasks are all "Scopes"
- **Unlimited Hierarchy**: No depth restrictions (default: unlimited; configurable via HierarchyPolicy)
- **Consistent Operations**: Same features available at every level
- **Human-Friendly Aliases**: Memorable identifiers like `quiet-river-x7k` instead of ULIDs

### AI-Native Architecture
- **Comment-Based AI Integration**: Asynchronous AI collaboration through comments
- **Workspace + Focus Management**: Automatic context switching based on directory
- **MCP (Model Context Protocol)**: Standard AI integration protocol
- **Natural Language Context**: "this", "that" resolve to focused scope

### Smart Alias System
- **Auto-Generated Names**: Memorable aliases using pattern like `bold-tiger-x7k`
- **Custom Aliases**: Add your own names like `auth-system` or `sprint-42`
- **Multiple Aliases**: One scope can have many aliases for different contexts
- **Case-Insensitive**: Type `AUTH` or `auth` - both work
- **Prefix Matching**: Type `qui` to match `quiet-river-x7k`

### Local-First Design
- **Offline-First**: All features work without internet
- **Selective Sync**: Choose what to share with external tools
- **Cross-Platform**: Native support for Windows, macOS, Linux
- **Privacy by Design**: Local-only data stays local

## 🚀 Quick Start

```bash
# Create a scope with auto-generated alias
$ scopes create "Implement authentication"
Created scope with canonical alias: quiet-river-x7k

# Create with custom alias
$ scopes create "User management" --alias users

# Add additional aliases
$ scopes alias add users user-system
$ scopes alias add users sprint-42

# Find scopes by alias
$ scopes show users
$ scopes show spr<TAB>  # Tab completion: sprint-42

# Create child scopes
$ scopes create "Login form" --parent users --alias login
$ scopes create "User profile" --parent users --alias profile

# View hierarchy
$ scopes tree users
users          User management
├── login      Login form
└── profile    User profile
```

## 📚 Documentation

### Getting Started
- [Tutorial: Getting Started with Aliases](docs/tutorials/getting-started-with-aliases.md) - Hands-on introduction
- [CLI Quick Reference](docs/reference/cli-quick-reference.md) - All commands at a glance
- [Using Aliases Guide](docs/guides/using-aliases.md) - Best practices and workflows

### Architecture & Design
- [Domain Overview](docs/explanation/domain-overview.md) - Core concepts and domain model
- [Clean Architecture](docs/explanation/clean-architecture.md) - Architectural layers and principles
- [Domain-Driven Design](docs/explanation/domain-driven-design.md) - DDD implementation approach
- [Architecture Decision Records](docs/explanation/adr/) - Key architectural decisions
- [Use Case Style Guide](docs/guides/use-case-style-guide.md) - Implementation patterns

### Reference
- [CLI Alias Commands](docs/reference/cli-alias-commands.md) - Complete alias command reference

## 🏗️ Architecture

### Module Structure

The project follows Clean Architecture with clear separation of concerns:

#### Platform Modules
- **:platform:commons**: Pure abstractions and primitive types (interfaces, type aliases)
- **:platform:domain-commons**: Domain-specific helpers shared across bounded contexts
- **:platform:application-commons**: Application layer utilities and common use case patterns
- **:platform:infrastructure**: System resource implementations (DB, time providers, ID generators)

#### Bounded Contexts
Each context follows DDD with three layers:
- **domain**: Business logic, entities, value objects, domain services
- **application**: Use cases, DTOs, application services
- **infrastructure**: Repositories, external service adapters

#### Cross-Cutting Concerns
- **contracts-***: Inter-context communication interfaces
- **interfaces-***: User-facing adapters (CLI, API)

For detailed dependency rules, see [Architecture Guidelines](docs/architecture/guidelines/dependency-rules.md).

## 🤝 Contributing

This is a greenfield project inheriting the best ideas from Project Manager while introducing the revolutionary unified Scope concept for true AI-developer symbiosis.

### Development Guidelines

1. Follow Clean Architecture and DDD principles
2. Write property-based tests for value objects and events
3. Run `./gradlew konsistTest` to verify architectural compliance
4. Document architectural decisions in ADRs
5. Respect module dependency rules - see [dependency-rules.md](docs/architecture/guidelines/dependency-rules.md)
6. Run static checks: `./gradlew detekt ktlintFormat`

## 📄 License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

## 🌟 Target Users

- **AI-Driven Developers**: Using AI as primary development partner
- **Tech Leads**: Managing design quality before team presentation
- **OSS Contributors**: Coordinating across multiple projects
- **Multi-Device Developers**: Seamless work across machines
- **International Engineers**: Breaking language barriers with AI
