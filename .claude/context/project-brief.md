---
created: 2025-08-25T14:33:19Z
last_updated: 2025-08-25T14:33:19Z
version: 1.0
author: Claude Code PM System
---

# Project Brief

## What is Scopes?

Scopes is a next-generation task and project management tool that fundamentally reimagines how developers and AI assistants collaborate. By treating every project element as a unified "Scope" entity and providing native AI integration, it eliminates the friction of context switching and enables true symbiotic development.

## Why Does It Exist?

### The Problem
Modern developers increasingly rely on AI assistants, but current tools create constant friction:
- **Context Loss**: Every new chat session starts from zero
- **Manual Handoffs**: Explaining project state repeatedly to AI
- **Tool Fragmentation**: Task management separate from AI interaction
- **Synchronization Issues**: Local work vs cloud services
- **Privacy Concerns**: Forced cloud dependency for basic features

### The Opportunity
AI-driven development is becoming the norm, not the exception. Developers need tools designed for this reality, where:
- AI assistants are primary collaborators, not occasional helpers
- Context preservation is automatic, not manual
- Privacy and local-first operation are fundamental rights
- Task organization enhances AI effectiveness

## Core Innovation

### Unified Scope Concept
Instead of projects, epics, stories, and tasks as separate entities, everything is a "Scope":
- **Infinitely Recursive**: A scope can contain any number of child scopes
- **Consistently Powerful**: Every scope has the same capabilities
- **Naturally Hierarchical**: Matches how developers think about work

### AI-Native Design
Built from the ground up for AI collaboration:
- **Comment Threads**: Asynchronous AI interaction with full history
- **Automatic Context**: AI understands current scope and its hierarchy
- **Natural References**: "this", "parent", "related" resolve intelligently
- **Multi-Assistant**: Different AIs can collaborate on the same scope

### Local-First Architecture
Privacy and performance by design:
- **Offline-First**: Full functionality without internet
- **Selective Sync**: User controls what goes to cloud
- **Fast Operations**: No network latency for core features
- **Data Ownership**: User's data stays on user's machine

## Success Criteria

### Short Term (3 months)
- **MVP Release**: Core CLI with scope management
- **AI Integration**: MCP protocol implementation
- **Local Storage**: Fast, reliable local database
- **Basic Sync**: Optional GitHub integration

### Medium Term (6 months)
- **Multi-Platform**: Windows, macOS, Linux native support
- **Advanced AI**: Multiple assistant support
- **External Integrations**: Jira, Linear, GitHub Issues
- **Performance**: Sub-millisecond operations

### Long Term (12 months)
- **Ecosystem**: Plugin architecture for extensions
- **Team Features**: Shared scopes with permissions
- **AI Marketplace**: Specialized assistants for different domains
- **Enterprise**: Self-hosted options with compliance

## Design Principles

### 1. Simplicity Through Unity
One concept (Scope) that scales from simple tasks to complex projects

### 2. AI as First-Class Citizen
Every feature designed with AI collaboration in mind

### 3. Local-First, Cloud-Optional
Privacy and ownership are non-negotiable

### 4. Developer Ergonomics
CLI-first interface that feels natural to developers

### 5. Zero Configuration
Smart defaults with optional customization

## Technical Approach

### Architecture
- **Clean Architecture**: Clear separation of concerns
- **Domain-Driven Design**: Rich domain model
- **Event-Driven**: Reactive updates and integrations
- **Functional Core**: Pure, testable business logic

### Technology Stack
- **Language**: Kotlin for type safety and modern features
- **Runtime**: JVM with GraalVM native image support
- **Storage**: Local SQLite with optional sync
- **AI Protocol**: MCP (Model Context Protocol)

### Quality Standards
- **Test Coverage**: Comprehensive unit and integration tests
- **Architecture Tests**: Automated architecture validation
- **Performance**: Benchmarked critical operations
- **Security**: Local encryption, secure sync protocols

## Project Scope

### In Scope
- Core scope management (CRUD operations)
- AI integration via MCP
- Local storage and retrieval
- CLI interface
- Basic external sync (GitHub)
- Multi-platform support

### Out of Scope (Initially)
- Web interface
- Mobile applications
- Real-time collaboration
- Built-in AI models
- Enterprise features
- Paid cloud services

## Risk Mitigation

### Technical Risks
- **Performance**: Benchmarking from day one
- **Compatibility**: Testing across platforms early
- **AI Protocol Changes**: Abstraction layer for flexibility

### Market Risks
- **Adoption**: Focus on power users first
- **Competition**: Unique AI-native approach
- **Monetization**: Open source with optional services

### Execution Risks
- **Scope Creep**: Clear MVP definition
- **Complexity**: Incremental feature addition
- **Quality**: Automated testing and validation
