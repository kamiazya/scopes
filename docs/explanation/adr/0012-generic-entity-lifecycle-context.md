# ADR 0012: Generic Entity Lifecycle Context

## Status

Accepted

## Context

The current Scopes architecture handles entity management on a per-context basis, with each bounded context (Scope Management, User Preferences, Device Synchronization) implementing its own approach to:
- Change tracking and history
- Version management
- State persistence
- AI collaboration capabilities

This leads to several architectural challenges:

1. **Inconsistent User Experience**: Different entities have different capabilities for versioning, history, and AI suggestions
2. **Code Duplication**: Similar patterns reimplemented across contexts
3. **Limited Extensibility**: Adding new entity types requires significant development effort
4. **AI Integration Complexity**: Each entity type needs custom AI integration

Inspired by the Lix project's approach to generic change control, we recognize the need for a unified entity lifecycle management system that can work with any entity type.

## Decision

We will create a **Generic Entity Lifecycle Context** that provides universal change management capabilities for all entity types in the system.

### Core Principles

1. **Entity Agnostic**: The system works with any entity type through a generic interface, treating all entities equally whether they are Scopes, User Preferences, or future entity types.

2. **Change as First-Class Citizen**: Every modification to any entity is captured as a discrete, attributable change with full context about what changed, when, why, and by whom.

3. **Version Control Paradigm**: Adopt Git-like concepts (branches, merges, history) for entity management, enabling safe experimentation and rollback capabilities.

4. **AI-Ready Architecture**: Built-in support for AI agents to analyze entities and propose changes through a structured workflow.

### Architectural Approach

This will be implemented as a **Shared Kernel** (DDD pattern), meaning:
- It's a carefully designed subset of the domain model shared by multiple bounded contexts
- All contexts can depend on and use these capabilities directly
- Changes to the shared kernel require coordination across all dependent contexts

### Key Capabilities

1. **Universal Change Tracking**: Track field-level changes for any entity type using a path-based system (similar to JSONPath)

2. **Version Management**: Create branches of entities for experimentation, with merge capabilities and conflict resolution

3. **Complete Audit Trail**: Maintain full history of all changes with attribution (User, AI Agent, or System)

4. **Snapshot System**: Store complete entity states at specific points for fast reconstruction and comparison

5. **Pluggable AI Integration**: Allow AI strategies to be registered per entity type for analysis and proposals

## Consequences

### Positive
- **Unified Experience**: Users get consistent versioning and history features across all entity types
- **Reduced Complexity**: New entity types automatically inherit change management capabilities
- **Enhanced Collaboration**: AI agents can work with any entity type through a standard interface
- **Improved Auditability**: System-wide change tracking for compliance and debugging
- **Future Flexibility**: Foundation for advanced features like real-time collaboration

### Negative
- **Shared Kernel Coupling**: All contexts become coupled to the shared kernel's design
- **Migration Complexity**: Existing entities need retrofitting to use the new system
- **Performance Considerations**: Generic serialization may be slower than type-specific approaches
- **Coordination Overhead**: Changes to shared kernel require cross-team coordination

### Neutral
- **Storage Requirements**: Additional tables for changes, versions, and snapshots
- **Learning Curve**: Teams need to understand the new lifecycle patterns
- **Testing Strategy**: Need comprehensive test coverage for generic scenarios

## Alternatives Considered

1. **Per-Context Implementation**: Continue with separate implementations
   - Rejected: Leads to inconsistency and duplication

2. **External Service**: Use a separate microservice for change management
   - Rejected: Violates local-first principles, adds network complexity

3. **Event Sourcing Only**: Use pure event sourcing without snapshots
   - Rejected: Performance concerns for entity reconstruction

## Related Decisions

- [ADR 0007: Domain-Driven Design](0007-domain-driven-design-adoption.md) - Shared Kernel pattern
- [ADR 0008: Clean Architecture](0008-clean-architecture-adoption.md) - Layer separation
- [ADR 0002: AI-Driven Development](0002-ai-driven-development-architecture.md) - AI integration needs
- [ADR 0001: Local-First Architecture](0001-local-first-architecture.md) - Local change tracking

## Implementation Notes

Implementation will follow a phased approach:
1. Define core abstractions and interfaces
2. Build infrastructure components
3. Migrate existing entities incrementally
4. Add advanced features (merging, AI proposals)

Detailed implementation patterns and code examples are maintained separately in the implementation guide.

## Tags

`entity-lifecycle`, `shared-kernel`, `change-management`, `versioning`, `ai-integration`
