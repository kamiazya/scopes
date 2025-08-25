---
created: 2025-08-25T14:33:19Z
last_updated: 2025-08-25T14:33:19Z
version: 1.0
author: Claude Code PM System
---

# Project Overview

## Executive Summary

Scopes is a groundbreaking task and project management system that revolutionizes developer-AI collaboration through its unified "Scope" concept and local-first architecture. By treating AI assistants as first-class collaborators and preserving context automatically, it eliminates the friction that developers face when working with AI tools.

## Current State

### Development Status
- **Phase**: Pre-MVP Development
- **Architecture**: Established (Clean Architecture + DDD)
- **Core Domain**: Implemented (Scope entity, Value Objects)
- **Infrastructure**: In Progress (CLI, Storage, MCP)

### Completed Features
- Domain model with Scope entity
- Hierarchy policy (unlimited depth)
- Value objects (ScopeId, Title, Description)
- Property-based testing framework
- Architecture validation (Konsist)
- Build system with multi-module support

### In Progress
- CLI interface implementation
- Local storage adapter
- MCP protocol integration
- External sync capabilities

## Feature Categories

### 1. Scope Management
Core functionality for creating and organizing work:

- **Create Scope**: Initialize new scope with metadata
- **Nested Scopes**: Unlimited parent-child relationships
- **Scope Navigation**: Move between scopes naturally
- **Scope Status**: Track progress (planning, active, complete)
- **Scope Templates**: Reusable scope structures
- **Bulk Operations**: Manage multiple scopes efficiently

### 2. AI Integration
Native support for AI assistant collaboration:

- **Comment System**: Threaded AI conversations
- **Context Injection**: Automatic scope context for AI
- **MCP Protocol**: Standard AI tool integration
- **Assistant Handoff**: Seamless context transfer
- **AI History**: Full conversation preservation
- **Multi-Assistant**: Support for different AI providers

### 3. Workspace Management
Intelligent organization and context switching:

- **Auto-Detection**: Current directory awareness
- **Focus Management**: Active scope tracking
- **Workspace Isolation**: Separate project contexts
- **Quick Switch**: Instant context changes
- **Workspace Templates**: Predefined configurations
- **Cross-Workspace**: Shared scopes between workspaces

### 4. Local-First Storage
Privacy-preserving data management:

- **Local Database**: SQLite for fast access
- **Encryption**: Optional local encryption
- **Backup/Restore**: Data portability
- **Import/Export**: Various format support
- **Versioning**: Scope history tracking
- **Conflict Resolution**: Smart merge capabilities

### 5. External Integration
Connect with existing tools and services:

- **GitHub Sync**: Issues and PRs as scopes
- **Jira Bridge**: Bidirectional sync (planned)
- **Linear Connect**: Modern tool integration (planned)
- **Git Integration**: Commit context awareness
- **API Access**: RESTful API for extensions
- **Webhook Support**: Real-time updates

### 6. Developer Experience
CLI-first interface optimized for developers:

- **Natural Commands**: Intuitive syntax
- **Shell Integration**: Autocomplete support
- **Keyboard Shortcuts**: Efficient navigation
- **Rich Output**: Formatted, colorized display
- **Scriptable**: Automation-friendly
- **Configuration**: Extensive customization

## Integration Points

### Development Tools
- **IDEs**: VS Code, IntelliJ plugins (planned)
- **Terminals**: Native shell integration
- **Git**: Commit message templates
- **CI/CD**: Automation triggers

### AI Assistants
- **Claude**: Primary integration via MCP
- **GPT**: OpenAI API support (planned)
- **Local Models**: Ollama integration (planned)
- **Custom**: Plugin architecture for others

### External Services
- **Issue Trackers**: GitHub, Jira, Linear
- **Documentation**: Markdown export
- **Analytics**: Usage metrics export
- **Backup**: Cloud storage providers

## Technical Capabilities

### Performance
- Sub-millisecond scope operations
- Instant context switching
- Minimal memory footprint
- Fast startup time
- Efficient sync protocols

### Scalability
- Handles thousands of scopes
- Unlimited hierarchy depth
- Concurrent operations
- Incremental sync
- Lazy loading

### Reliability
- ACID transactions
- Automatic backups
- Crash recovery
- Data validation
- Error resilience

### Security
- Local data encryption
- Secure sync protocols
- API authentication
- Permission system
- Audit logging

## Platform Support

### Operating Systems
- **Linux**: Full support (x86_64, ARM64)
- **macOS**: Full support (Intel, Apple Silicon)
- **Windows**: Full support (x64, ARM64)

### Package Managers
- **Homebrew**: macOS/Linux installation
- **APT/YUM**: Linux packages
- **Scoop/Chocolatey**: Windows packages
- **Direct Download**: Standalone binaries

### Runtime Options
- **JVM**: Standard distribution
- **Native**: GraalVM compiled binaries
- **Container**: Docker images

## Deployment Architecture

### Local Installation
```
User Machine
├── Scopes CLI
├── Local Database
├── Configuration
└── Workspace Data
```

### Optional Sync
```
Local ←→ Sync Service ←→ External Services
  ↓                           ↓
Database                  GitHub/Jira/etc
```

### Future Team Features
```
Team Member A ←→ Shared Scope ←→ Team Member B
      ↓              Server              ↓
Local Cache                        Local Cache
```

## Success Metrics

### User Experience
- 5-second onboarding to first scope
- 90% commands require no documentation
- Zero data loss guarantee
- 100ms response time for all operations

### Adoption
- 10,000 active developers (Year 1)
- 1M scopes created (Year 1)
- 50% daily active usage
- 4.5+ star average rating

### Technical
- 99.9% uptime for sync services
- <1% CPU usage idle
- <50MB memory usage
- <10MB disk per 1000 scopes
