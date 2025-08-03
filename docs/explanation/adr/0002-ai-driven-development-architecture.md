# ADR-0002: Adopt AI-Driven Development Architecture

## Status

Accepted

## Context

Scopes is designed specifically to support AI-assisted development workflows. As AI tools become integral to modern software development, there is a need for a system that facilitates seamless collaboration between humans and AI assistants while maintaining clear accountability and traceability.

Key requirements driving this decision:

- Support for multiple AI assistants working on the same project
- Clear audit trail of AI-initiated changes
- Integration with existing AI tools and platforms
- Maintenance of human oversight and responsibility

## Decision

**Adopt an AI-driven development architecture where AI assistants are first-class citizens in the development workflow, integrated through Model Context Protocol (MCP) as the primary interface.**

### Key Design Principles

#### 1. MCP as Standard Integration

- **Requirement**: MCP is mandatory as the de facto standard for AI integration
- **Scope**: Provides full CLI-equivalent capabilities to AI assistants
- **Flexibility**: Allows exceptions for specific use cases when needed

#### 2. AI-Optional Operation

- **Core Functionality**: All basic features work without AI assistance
- **Enhancement**: AI provides enhanced capabilities, not dependencies
- **Degradation**: Graceful fallback when AI is unavailable

#### 3. Git-like Co-authorship Model

- **Attribution**: Track both AI agent and human instructor
- **Format**: Similar to Git's co-author model (e.g., "Co-authored-by:")
- **Responsibility**: Human users retain full responsibility per AI tool usage principles

#### 4. Comprehensive Change Tracking

- **Version Control**: All changes are versioned with appropriate granularity
- **Audit Logging**: Complete audit trail of who changed what and when
- **Co-authorship**: Record which human instructed which AI for each change

## Consequences

### Positive

- **Enhanced Productivity**: AI assistants can work efficiently through standardized interface
- **Clear Accountability**: Git-like co-authorship provides clear attribution
- **Flexibility**: AI-optional design ensures system remains usable in all contexts
- **Future-proof**: MCP adoption aligns with industry standards
- **Trust**: Comprehensive audit trails build confidence in AI-assisted changes

### Negative

- **Complexity**: Additional metadata tracking for co-authorship
- **Storage**: Version history and audit logs require more storage
- **Learning Curve**: Users need to understand AI collaboration model
- **MCP Dependency**: Tied to MCP as primary integration method

### Neutral

- **Implementation Flexibility**: Diff-based or snapshot versioning based on complexity
- **Permission Model**: AI has same permissions as instructing user
- **Integration Scope**: New AI platforms require MCP support

## Alternatives Considered

### Alternative 1: Custom AI Integration Protocol

- **Description**: Develop proprietary protocol for AI integration
- **Rejection reason**: MCP is already the de facto standard, custom protocol would limit adoption

### Alternative 2: Plugin-based Architecture Only

- **Description**: AI integration through plugins without standardized protocol
- **Rejection reason**: Lacks consistency and would require multiple implementations

### Alternative 3: Read-only AI Access

- **Description**: AI can only read and suggest, not write
- **Rejection reason**: Severely limits AI productivity benefits

## Related Decisions

- ADR-0001: Local-First Architecture (AI operates on local data)

## Scope

- **Bounded Context**: AI Integration, Audit System, Version Control
- **Components**: MCP Server, Audit Logger, Version Manager
- **External Systems**: AI platforms (Claude, GPT, Gemini, local LLMs)

## Implementation Notes

### Version Control Strategy

```
# Preference order:
1. Diff-based storage (if implementation complexity is manageable)
2. Snapshot-based storage (fallback for simplicity)

# Implementation approach:
- Start with snapshot-based storage for rapid development
- Consider migration to diff-based storage as storage efficiency becomes critical
```

### Co-authorship Format

```json
{
  "author": "user123",
  "co_authors": [
    {
      "type": "ai",
      "agent": "claude-3",
      "instructed_by": "user123"
    }
  ],
  "timestamp": "2024-01-01T12:00:00Z",
  "changes": [...]
}
```

### MCP Capabilities

- **Principle**: AI has access to all CLI operations
- **Exceptions**: Specific operations may have additional considerations
- **Flexibility**: Framework allows for operation-specific policies

### Implementation Priority

1. **Phase 1**: MCP server with basic CRUD operations
2. **Phase 2**: Audit logging and co-authorship tracking
3. **Phase 3**: Version control system
4. **Phase 4**: Advanced AI operations (bulk changes, migrations)
5. **Phase 5**: Multi-AI coordination features

### Security Considerations

- All AI operations are logged for security audit
- Human instructor is recorded for accountability
- Version control enables rollback of problematic changes
- **Safeguards for destructive operations**: Consider implementing user confirmation steps before AI executes potentially destructive operations (e.g., file deletion, bulk overwrites)
- **Operation risk assessment**: Categorize operations by risk level to determine appropriate safeguards

## Tags

`architecture`, `ai-integration`, `mcp`, `audit`, `version-control`
