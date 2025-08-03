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

## Tags

`architecture`, `ai-integration`, `mcp`, `audit`, `version-control`
