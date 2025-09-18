# CLAUDE.md - AI Context Engineering Hub

This file provides comprehensive context and guidance to Claude Code (claude.ai/code) when working with the Scopes project. It serves as the primary navigation hub for AI collaboration.

## 🗺️ Quick Navigation

### Essential Documents
- **Project Architecture**: @docs/explanation/clean-architecture.md
- **Domain Model**: @docs/reference/domain-model/
- **API Reference**: @docs/reference/api/
- **Testing Guide**: @docs/guides/architecture-testing-guide.md

### For Common Tasks
- **Adding Features**: @docs/guides/use-case-style-guide.md
- **Running Tests**: `./gradlew test` or `./gradlew konsistTest`
- **Building Project**: `./gradlew build`
- **MCP Server**: @docs/reference/mcp-implementation-guide.md

### Implementation Status
- ✅ **Working**: Basic CRUD, Aliases, Aspects, Contexts (backend), MCP Server
- 🚧 **Partial**: Context CLI commands, Complex aspect queries
- ❌ **Not Implemented**: Focus management, Tree display, Status command
- 📦 **Infrastructure Only**: Event Sourcing, Device Sync (no user features)

## 📂 Project Structure Map

```
scopes/
├── contexts/               # Bounded Contexts (DDD)
│   ├── scope-management/   # Core scope operations
│   ├── user-preferences/   # User settings
│   ├── device-synchronization/ # (Infrastructure only)
│   └── event-store/        # (Infrastructure only)
├── interfaces/
│   ├── cli/               # CLI commands implementation
│   └── mcp/               # MCP server (11 tools implemented)
├── platform/              # Cross-cutting concerns
│   ├── observability/     # Logging, metrics
│   └── application-commons/
├── apps/
│   ├── scopes/           # CLI application
│   └── scopesd/          # Daemon (skeleton only)
└── quality/
    └── konsist/          # Architecture tests
```

## 🎯 Project Overview

**Scopes** is a next-generation local-first task and project management tool designed for symbiotic collaboration between developers and AI assistants.

### Vision

Create a unified, recursive task management system where AI and human developers work together seamlessly, eliminating context loss and maximizing productivity through intelligent workspace management.

### Core Concepts

#### 1. Unified "Scope" Entity
- **Recursive Structure**: Projects, epics, and tasks are all "Scopes"
- **Unlimited Hierarchy**: No depth restrictions
- **Consistent Operations**: Same features at every level

#### 2. AI-Native Architecture
- **Comment-Based AI Integration**: Asynchronous AI collaboration through comments
- **Workspace + Focus Management**: Automatic context switching
- **MCP (Model Context Protocol)**: Standard AI integration
- **Natural Language Context**: "this", "that" resolve to focused scope

#### 3. Local-First Design
- **Offline-First**: All features work without internet
- **Selective Sync**: Choose what to share with external tools
- **Cross-Platform**: Native support for Windows, macOS, Linux
- **Privacy by Design**: Local-only data stays local

### Target Users

- **AI-Driven Developers**: Using AI as primary development partner
- **Tech Leads**: Managing design quality before team presentation
- **OSS Contributors**: Coordinating across multiple projects
- **Multi-Device Developers**: Seamless work across machines
- **International Engineers**: Breaking language barriers with AI

## 🏗️ Architecture & Patterns

### Clean Architecture Layers
1. **Domain**: Pure business logic, no external dependencies
2. **Application**: Use cases and command/query handlers
3. **Infrastructure**: Database, external services
4. **Interface**: CLI, MCP, future Web API

### Key Technologies
- **Language**: Kotlin with coroutines
- **Build**: Gradle with convention plugins
- **Database**: SQLite (local-first)
- **Testing**: Kotest + Konsist (architecture tests)
- **Error Handling**: Arrow's Either type
- **DI**: Koin for dependency injection

### Architectural Decisions
See @docs/explanation/adr/ for all Architecture Decision Records:
- ADR-0001: Local-first architecture
- ADR-0007: Domain-driven design
- ADR-0008: Clean architecture
- ADR-0011: Functional DDD
- ADR-0015: Event sourcing (future)

## 📚 Documentation Index

### By Purpose (Diátaxis Framework)

#### Learning (Tutorials)
- @docs/tutorials/getting-started.md - Complete beginner guide
- @docs/tutorials/working-with-contexts.md - Context system tutorial
- @docs/tutorials/getting-started-with-aliases.md - Alias deep dive

#### Doing (How-to Guides)
- @docs/guides/using-aliases.md - Alias best practices
- @docs/guides/observability-guide.md - Logging and monitoring
- @docs/guides/architecture-testing-guide.md - Konsist usage
- @docs/guides/development-guidelines.md - Contributing

#### Looking Up (Reference)
- @docs/reference/cli-quick-reference.md - All CLI commands
- @docs/reference/mcp-implementation-guide.md - MCP tools (11 implemented)
- @docs/reference/domain-model/ - Technical specifications

#### Understanding (Explanation)
- @docs/explanation/clean-architecture.md - Architecture principles
- @docs/explanation/domain-driven-design.md - DDD implementation
- @docs/explanation/event-sourcing-architecture.md - Event store (foundation only)
- @docs/explanation/device-synchronization.md - Sync design (not implemented)

## 🤖 AI Collaboration Guidelines

### Context Engineering Best Practices

#### 1. Prime Context Before Major Tasks
```
Read @docs/explanation/clean-architecture.md
Read @docs/reference/domain-model/
Understand the bounded context structure
```

#### 2. Use Sub-Agents for Context Optimization

##### Always use the file-analyzer sub-agent when asked to read files
The file-analyzer agent is an expert in extracting and summarizing critical information from files, particularly log files and verbose outputs. It provides concise, actionable summaries that preserve essential information while dramatically reducing context usage.

##### Always use the code-analyzer sub-agent when asked to search code, analyze code, research bugs, or trace logic flow
The code-analyzer agent is an expert in code analysis, logic tracing, and vulnerability detection. It provides concise, actionable summaries that preserve essential information while dramatically reducing context usage.

##### Always use the test-runner sub-agent to run tests and analyze the test results
Using the test-runner agent ensures:
- Full test output is captured for debugging
- Main conversation stays clean and focused
- Context usage is optimized
- All issues are properly surfaced
- No approval dialogs interrupt the workflow

#### 3. Follow Established Patterns
Check existing implementations before creating new ones:
- Command handlers: @contexts/*/application/src/main/kotlin/*/command/handler/
- Query handlers: @contexts/*/application/src/main/kotlin/*/query/handler/
- CLI commands: @interfaces/cli/src/main/kotlin/*/commands/

### Common AI Tasks

#### Adding a New Feature
1. Read @docs/guides/use-case-style-guide.md
2. Check existing similar implementations
3. Follow the command/query pattern
4. Add tests following existing patterns
5. Run `./gradlew konsistTest` to verify architecture

#### Fixing a Bug
1. Use code-analyzer to trace the issue
2. Check related tests for context
3. Fix following existing patterns
4. Add regression test
5. Verify with `./gradlew test`

#### Understanding Codebase
1. Start with @docs/explanation/domain-overview.md
2. Explore specific bounded context
3. Check interfaces for public API
4. Review tests for usage examples

## 🛠️ Development Workflow

### Essential Commands
```bash
# Build and test
./gradlew build              # Full build with tests
./gradlew test               # Unit tests only
./gradlew konsistTest        # Architecture tests
./gradlew check             # All checks

# Run application
./gradlew :apps-scopes:run --args="list"
./gradlew :apps-scopes:run --args="mcp"  # Start MCP server

# Code quality
./gradlew detekt            # Static analysis
./gradlew spotlessApply     # Format code
```

### Current Build Issues
- **SQLite JDBC**: Native library issues in Termux environment
- **Workaround**: Use `--no-verify` for git commits if pre-commit fails

## 🔍 Quick Reference

### MCP Tools (11 Implemented)
1. `scopes.create` - Create new scope
2. `scopes.get` - Get scope details
3. `scopes.update` - Update scope
4. `scopes.delete` - Delete scope
5. `scopes.children` - List children
6. `scopes.roots` - List root scopes
7. `aliases.add` - Add alias
8. `aliases.remove` - Remove alias
9. `aliases.setCanonical` - Set canonical alias
10. `aliases.list` - List aliases
11. `aliases.resolve` - Resolve alias

### Missing Features (Documented but Not Implemented)
- Focus management system (`scopes focus`)
- Tree visualization (`scopes tree`)
- Status display (`scopes status`)
- Device synchronization (infrastructure only)
- Event sourcing user features (infrastructure only)

## 🎓 Learning Path

For new AI assistants working on Scopes:

1. **Understand the Vision**: Read project overview above
2. **Learn the Architecture**: @docs/explanation/clean-architecture.md
3. **Explore Domain Model**: @docs/reference/domain-model/
4. **Review Implementation**: Check a complete feature flow
5. **Understand Testing**: @docs/guides/architecture-testing-guide.md

## 💬 Language and Communication

### Official Project Language: English
- All documentation, code, and public content in English
- Ensures international accessibility
- Exception: Local configuration files (e.g., CLAUDE.local.md)

### AI Interaction Language: User Preference
- Claude interactions in user's preferred language (日本語OK)
- Local notes can be in any language
- Balances accessibility with productivity

## 🚨 Critical Rules

### Philosophy
> Think carefully and implement the most concise solution that changes as little code as possible.

### Quality Standards
- Run `./gradlew konsistTest` to verify architectural compliance
- Architecture tests validate Clean Architecture and DDD principles
- All changes must pass Konsist architecture validation

### Error Handling Guidelines
Follow the error handling patterns defined in [Error Handling Guidelines](./docs/guidelines/error-handling.md).

Key points:
- Use Kotlin's `error()`, `check()`, `require()` instead of throwing exceptions directly
- Never use "unknown" or default fallbacks that mask data corruption
- Use Arrow's Either for functional error handling
- Fail-fast for data integrity issues

### Error Handling Approach

- **Fail fast** for critical configuration (missing text model)
- **Log and continue** for optional features (extraction model)
- **Graceful degradation** when external services unavailable
- **User-friendly messages** through resilience layer

### Testing Philosophy
- Always use the test-runner agent to execute tests
- Do not use mock services for anything ever
- Do not move on to the next test until the current test is complete
- If the test fails, consider checking if the test is structured correctly before deciding we need to refactor the codebase
- Tests to be verbose so we can use them for debugging

## 📋 Absolute Rules

- **NO PARTIAL IMPLEMENTATION** - Complete features or don't start
- **NO SIMPLIFICATION** - No "simplified for now" comments
- **NO CODE DUPLICATION** - Check existing codebase first
- **NO DEAD CODE** - Either use or delete completely
- **IMPLEMENT TESTS** - Every function needs tests
- **NO CHEATER TESTS** - Tests must reveal real flaws
- **NO INCONSISTENT NAMING** - Follow existing patterns
- **NO OVER-ENGINEERING** - Simple solutions first
- **NO MIXED CONCERNS** - Proper separation of concerns
- **NO RESOURCE LEAKS** - Clean up all resources

## 🤝 Tone and Behavior

- Criticism is welcome - tell me when I'm wrong
- Suggest better approaches when you see them
- Point out relevant standards I might have missed
- Be skeptical and ask questions
- Be concise unless working through details
- Skip flattery and unnecessary compliments
- Ask for clarification instead of guessing

## 🔗 External Resources

### Documentation
- [Diátaxis Framework](https://diataxis.fr/) - Documentation structure
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)

### Technologies
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Arrow](https://arrow-kt.io/) - Functional programming
- [Konsist](https://docs.konsist.lemonappdev.com/) - Architecture testing
- [MCP Protocol](https://modelcontextprotocol.io/) - AI integration

---

Remember: This is a greenfield project inheriting the best ideas from Project Manager while introducing the revolutionary unified Scope concept for true AI-developer symbiosis.
