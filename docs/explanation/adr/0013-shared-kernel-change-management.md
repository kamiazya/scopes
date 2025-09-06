# ADR 0013: Shared Kernel for Change Management

## Status

Accepted

## Context

Following [ADR 0012: Generic Entity Lifecycle Context](0012-generic-entity-lifecycle-context.md), we need to decide how multiple Bounded Contexts will integrate with the generic change management capabilities.

The traditional DDD approach would use integration patterns like:
- **Customer/Supplier**: Contexts negotiate interfaces
- **Anti-Corruption Layer**: Each context translates external concepts
- **Published Language**: Define a common integration language
- **Separate Ways**: Each context implements its own solution

However, for change management specifically, these patterns introduce complexity:
- Multiple interpretations of "what is a change"
- Synchronization overhead between contexts
- Duplicate infrastructure for common operations
- Inconsistent user experience across entity types

## Decision

We will implement the Entity Lifecycle functionality as a **Shared Kernel** - a carefully selected subset of the domain model that multiple bounded contexts agree to share.

### Why Shared Kernel

Change management represents a **fundamental cross-cutting concern** that:
1. Requires identical behavior across all contexts
2. Benefits from shared infrastructure (storage, querying)
3. Needs consistent user experience
4. Forms the foundation for system-wide features (audit, AI collaboration)

### Shared Kernel Scope

The shared kernel will include:

1. **Core Concepts**: What constitutes a change, version, and snapshot
2. **Value Objects**: Common data structures for representing changes
3. **Service Interfaces**: Contracts for change recording and querying
4. **Domain Events**: Standardized events for change notifications
5. **Repository Abstractions**: Interfaces for persistence (not implementations)

### Shared Kernel Boundaries

What **IS** in the shared kernel:
- Generic change representation (field path, operation type, before/after values)
- Version management concepts (branch, merge, history)
- Attribution model (who made changes: User, AI, System)
- Query interfaces for retrieving changes and versions

What **IS NOT** in the shared kernel:
- Entity-specific business logic
- Concrete persistence implementations
- UI/presentation concerns
- Entity serialization strategies
- Context-specific validation rules

### Integration Approach

Each bounded context integrates by:
1. **Implementing Change Detection**: Context-specific logic to identify what changed
2. **Providing Serialization**: How to convert entities to/from generic format
3. **Wrapping Repositories**: Adding change tracking to existing persistence
4. **Handling Events**: Responding to change notifications as needed

## Consequences

### Positive
- **Unified Behavior**: All contexts share identical change management semantics
- **Reduced Complexity**: No translation layers or integration adapters needed
- **Performance**: Direct in-process calls without serialization overhead
- **Rapid Adoption**: New contexts inherit proven change management patterns
- **Type Safety**: Shared types prevent integration errors at compile time

### Negative
- **Coupling**: All contexts become coupled to shared kernel design
- **Coordination Burden**: Changes require agreement across multiple teams
- **Versioning Complexity**: Shared kernel evolution affects all consumers
- **Testing Overhead**: Need to test combinations of contexts with shared kernel

### Neutral
- **Governance Required**: Need clear ownership and change process
- **Documentation Critical**: Shared concepts must be well-documented
- **Careful Design**: Shared kernel must remain stable and minimal

## Risk Mitigation

### Risk: Shared Kernel Bloat
Keep the kernel minimal by:
- Regular reviews to identify and remove unnecessary elements
- Clear criteria for what belongs in the kernel
- Resistance to adding context-specific features

### Risk: Breaking Changes
Manage evolution through:
- Semantic versioning
- Deprecation periods
- Comprehensive test suites
- Clear migration guides

### Risk: Team Conflicts
Establish governance:
- Designated kernel maintainers
- Change proposal process
- Regular cross-team meetings
- Clear escalation paths

## Alternatives Considered

1. **Separate Implementations**: Each context implements its own change tracking
   - Rejected: Leads to inconsistency and duplication

2. **External Service**: Centralized change management service
   - Rejected: Adds network complexity, violates local-first principles

3. **Event Streaming**: Use event store for all changes
   - Rejected: Overkill for local-first architecture, adds operational complexity

## Related Decisions

- [ADR 0012: Generic Entity Lifecycle Context](0012-generic-entity-lifecycle-context.md) - Establishes need for generic change management
- [ADR 0007: Domain-Driven Design](0007-domain-driven-design-adoption.md) - Shared Kernel pattern definition
- [ADR 0001: Local-First Architecture](0001-local-first-architecture.md) - Influences in-process integration

## Implementation Notes

The shared kernel will be implemented as a separate module that other contexts depend on. Each context remains responsible for its own domain logic while delegating change management to the shared kernel.

Detailed patterns for context integration are documented separately.

## Tags

`shared-kernel`, `ddd`, `integration`, `change-management`, `cross-cutting-concerns`
