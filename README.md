# Scopes

**Scopes** is a next-generation local-first task and project management tool designed for symbiotic collaboration between developers and AI assistants.

## 🎯 Vision

Create a unified, recursive task management system where AI and human developers work together seamlessly, eliminating context loss and maximizing productivity through intelligent workspace management.

## ✨ Core Features

### Unified "Scope" Entity
- **Recursive Structure**: Projects, epics, and tasks are all "Scopes"
- **Unlimited Hierarchy**: No depth restrictions (default: unlimited; configurable via HierarchyPolicy)
- **Consistent Operations**: Same features available at every level
- **Flexible Naming**: Each scope can have multiple aliases for easy reference
  - Auto-generated canonical aliases (e.g., `witty-penguin-42`)
  - Custom aliases (e.g., `my-project`, `feature-x`)
  - All CLI commands accept either scope IDs or aliases

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
