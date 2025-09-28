# Scopes Documentation

Welcome to the Scopes project documentation. This documentation follows the [Diátaxis framework](https://diataxis.fr/) for clear organization and navigation.

## 🚀 Quick Start

### New to Scopes?
1. Start with [Getting Started](./tutorials/getting-started.md) - Complete beginner's tutorial
2. Review the [CLI Quick Reference](./reference/cli-quick-reference.md) - All commands at a glance
3. Follow [Using Aliases Guide](./guides/using-aliases.md) - Best practices and workflows

### Need Help With...
- **Installing Scopes?** → [Getting Started Tutorial](./tutorials/getting-started.md)
- **Managing multiple projects?** → [Working with Contexts](./tutorials/working-with-contexts.md)
- **Understanding aliases?** → [Alias System Tutorial](./tutorials/getting-started-with-aliases.md)
- **Migrating from another tool?** → [Migration Guide](./guides/migrating-to-aliases.md)

## Documentation Categories

### 📚 [Tutorials](./tutorials/)

Learning-oriented guides for newcomers

Step-by-step learning materials to get you started with Scopes.

- [Getting Started](./tutorials/getting-started.md) - Complete introduction from installation to first use
- [Working with Contexts](./tutorials/working-with-contexts.md) - Learn to organize work with context views
- [Getting Started with Aliases](./tutorials/getting-started-with-aliases.md) - Deep dive into the alias system

### 📖 [How-to Guides](./guides/)

Task-oriented recipes for specific goals

Practical guides for accomplishing specific tasks and solving problems.

- [Using Aliases](./guides/using-aliases.md) - Best practices for alias management
- [Migrating to Aliases](./guides/migrating-to-aliases.md) - Transition from other systems
- [Observability Guide](./guides/observability-guide.md) - Logging, metrics, and monitoring
- [Architecture Testing Guide](./guides/architecture-testing-guide.md) - Automated architecture verification with Konsist
- [Use Case Style Guide](./guides/use-case-style-guide.md) - Guidelines for implementing use cases
- [Development Guidelines](./guides/development-guidelines.md) - Contributing and development practices

### 📋 [Reference](./reference/)

Information-oriented technical descriptions

Technical reference materials for quick lookup and detailed specifications.

- [CLI Quick Reference](./reference/cli-quick-reference.md) - All CLI commands
- [CLI Alias Commands](./reference/cli-alias-commands.md) - Comprehensive alias command reference
- [MCP Implementation Guide](./reference/mcp-implementation-guide.md) - Complete MCP server documentation
- [MCP Resources](./reference/mcp-resources.md) - MCP resource specifications
- [API Reference](./reference/api/) - Programmatic interfaces
- [Domain Model](./reference/domain-model/) - Technical domain specifications

### 💡 [Explanation](./explanation/)

Understanding-oriented conceptual discussions

In-depth explanations of concepts, architecture decisions, and design rationale.

- [Domain Overview](./explanation/domain-overview.md) - Core concepts and domain model
- [Alias System Architecture](./explanation/alias-system-architecture.md) - Technical design of aliases
- [Clean Architecture](./explanation/clean-architecture.md) - Architectural layers and principles
- [Domain-Driven Design](./explanation/domain-driven-design.md) - DDD implementation with functional programming
- [Event Sourcing Architecture](./explanation/event-sourcing-architecture.md) - Event-driven foundation (infrastructure only)
- [Device Synchronization](./explanation/device-synchronization.md) - Multi-device sync architecture (planned)
- [CLI↔Daemon gRPC Architecture (v1beta)](./explanation/cli-daemon-grpc.md) - Local IPC over gRPC, schema and plan
- [Architecture Decision Records](./explanation/adr/) - Documented architectural decisions
- [Automated Release Process](./explanation/automated-release-process.md) - CI/CD and release automation

## Key Features Documentation

### Alias System
The alias system is a core feature providing human-friendly identifiers:
- **Auto-generated aliases** like `quiet-river-x7k`
- **Custom aliases** like `auth-system` or `sprint-42`
- **Multiple aliases per scope** for different contexts
- **Case-insensitive** with smart prefix matching

Learn more:
- [Tutorial](./tutorials/getting-started-with-aliases.md) - Hands-on introduction
- [Guide](./guides/using-aliases.md) - Practical workflows
- [Reference](./reference/cli-alias-commands.md) - Command details
- [Architecture](./explanation/alias-system-architecture.md) - Technical design

## Contributing to Documentation

Please follow the Diátaxis framework when adding documentation:

- **Tutorials**: Learning journeys for beginners (step-by-step, complete)
- **Guides**: Step-by-step solutions to problems (task-focused, practical)
- **Reference**: Factual, structured information (comprehensive, accurate)
- **Explanation**: Context, alternatives, and reasoning (conceptual, thorough)

### Documentation Standards
- Use clear, concise language
- Include practical examples
- Test all code samples
- Keep information up-to-date
- Cross-reference related topics

## Feedback

Found an issue or have suggestions? Please file an issue in the project repository.
