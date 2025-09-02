# ADR 0015: Unified Event Sourcing Approach

## Status

Accepted

## Context

The introduction of the Generic Entity Lifecycle Context creates a need for comprehensive event tracking across all entity types. We need to capture:

- Every entity change with full context
- AI proposals and their resolutions
- Version branches and merges
- Complete audit trails for compliance

The existing Event Store Context provides domain-agnostic event storage, but we need to define how entity lifecycle events integrate with:
- Domain-specific events from various contexts
- Performance requirements for high-frequency changes
- AI attribution and learning needs
- Cross-entity correlations

## Decision

We will extend the existing Event Store Context with a **Unified Event Sourcing Approach** specifically designed for entity lifecycle management.

### Key Principles

1. **Event as Source of Truth**: All entity changes are derived from events, not direct mutations

2. **Immutable Event Log**: Events are append-only, providing complete audit history

3. **Snapshot Optimization**: Periodic snapshots enable fast entity reconstruction

4. **Context Bridging**: Entity lifecycle events can trigger domain-specific events

5. **AI Attribution**: Special event types capture AI proposals and decisions

### Event Categories

#### Entity Lifecycle Events
- **Entity Changed**: Records field-level changes with before/after values
- **Version Created**: New version branch creation
- **Version Merged**: Branch merge with conflict resolution
- **Entity Deleted**: Soft deletion with full state preservation

#### AI Collaboration Events
- **AI Proposal Created**: AI suggests changes with rationale
- **AI Proposal Resolved**: User accepts/rejects/modifies proposal
- **AI Learning Feedback**: User feedback for AI improvement

#### System Events
- **Snapshot Created**: Performance optimization checkpoints
- **Migration Applied**: Schema or data migrations
- **Integrity Verified**: Consistency checks

### Integration Patterns

1. **Event Projection**: Entity lifecycle events project to current state
2. **Event Correlation**: Related events linked by correlation IDs
3. **Event Enrichment**: Add context during event processing
4. **Event Filtering**: Query events by entity, version, time, or type

## Consequences

### Positive
- **Complete Auditability**: Every change is recorded with full context
- **Time Travel**: Reconstruct entity state at any point in history
- **AI Transparency**: Clear record of AI suggestions and decisions
- **Debugging Power**: Trace exact sequence of changes
- **Compliance Ready**: Audit logs for regulatory requirements

### Negative
- **Storage Requirements**: Events consume more space than state-only storage
- **Complexity**: Event sourcing adds conceptual overhead
- **Eventual Consistency**: Projections may lag behind events
- **Migration Difficulty**: Changing event schemas requires careful planning

### Neutral
- **Performance Trade-offs**: Write performance vs. read performance
- **Learning Curve**: Teams need to understand event sourcing
- **Tooling Requirements**: Need event viewers and debugging tools

## Architecture Integration

### With Entity Lifecycle Context
- Entity changes automatically generate events
- Events enable version reconstruction
- Snapshots optimize performance

### With AI Strategy Pattern
- AI proposals tracked as events
- User decisions recorded for learning
- Full attribution chain maintained

### With Shared Kernel
- Events flow across context boundaries
- Common event types in shared kernel
- Context-specific projections

## Performance Strategies

1. **Snapshotting**: Regular snapshots reduce reconstruction time
2. **Event Batching**: Group related changes in single transaction
3. **Async Processing**: Non-critical projections updated asynchronously
4. **Partitioning**: Events partitioned by entity type and time
5. **Archival**: Old events moved to cold storage

## Alternatives Considered

1. **State-Only Storage**: Traditional CRUD without events
   - Rejected: Loses audit trail and change context

2. **Change Data Capture**: Database-level change tracking
   - Rejected: Lacks semantic context and attribution

3. **Hybrid Approach**: Events for some entities, state for others
   - Rejected: Inconsistent user experience and complexity

4. **External Event Store**: Use dedicated event store service
   - Rejected: Violates local-first principles

## Related Decisions

- [ADR 0012: Generic Entity Lifecycle Context](0012-generic-entity-lifecycle-context.md) - Generates events
- [ADR 0013: Shared Kernel](0013-shared-kernel-change-management.md) - Defines event types
- [ADR 0014: AI Strategy Pattern](0014-pluggable-ai-strategy-pattern.md) - AI event requirements
- [ADR 0001: Local-First Architecture](0001-local-first-architecture.md) - Local event storage

## Implementation Notes

Event sourcing will be implemented incrementally:
1. Define core event types in shared kernel
2. Add event generation to entity lifecycle
3. Implement snapshot optimization
4. Build event query and projection systems

Detailed event schemas and processing patterns are documented separately.

## Tags

`event-sourcing`, `audit-trail`, `cqrs`, `entity-lifecycle`, `ai-attribution`
