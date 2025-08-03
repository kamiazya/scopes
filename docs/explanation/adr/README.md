# Architecture Decision Records (ADR)

> **Note**: For ADR creation guidelines and workflow integration, see [CONTRIBUTING.md](../../../CONTRIBUTING.md).

This directory contains Architecture Decision Records (ADRs) for the Scopes system. ADRs document the significant architectural decisions made during development, including the context, rationale, and consequences of each decision.

## What are ADRs

An Architecture Decision Record (ADR) is a document that captures an important architectural decision made along with its context and consequences. ADRs help preserve the reasoning behind architectural choices and provide future developers with the context needed to understand why certain decisions were made.

## ADR Format

All ADRs in this project follow a standardized format defined in [`TEMPLATE.md`](./TEMPLATE.md). Each ADR includes:

- **Status:** Current state of the decision (Accepted, Superseded)
- **Context:** The circumstances that led to this decision
- **Decision:** The specific choice made and its rationale
- **Consequences:** Expected positive and negative outcomes
- **Alternatives:** Other options considered and why they were rejected
- **Related Decisions:** Links to other ADRs or documents
- **Scope:** Which parts of the system are affected
- **Implementation Notes:** Practical guidance for implementation
- **Tags:** Keywords for categorization and search

### Status Guidelines

**Background:** In a Git-managed environment, the traditional "Proposed" status creates ambiguity. When an ADR is committed to the repository, it implies that a decision to document the architecture has already been made. Therefore, we adopt a simplified status model:

#### Status Values

- **Accepted**: The decision has been agreed upon and should be implemented. This is the initial status for all new ADRs.
- **Superseded by ADR-XXXX**: The decision has been replaced by another ADR. Include a reference to the new ADR.

#### Important Principle: Separation of Decision and Implementation

ADRs track architectural decisions, not implementation status. The lifecycle of a decision (Accepted → Superseded) is independent from the implementation lifecycle (planned → in-progress → completed → migrating → retired). Implementation status should be tracked through task management tools, not ADR statuses.

#### Workflow

1. **Pre-ADR Discussion**: Architecture proposals are discussed in GitHub Issues, Pull Requests, or team meetings.
2. **ADR Creation**: Once a decision is reached, create an ADR with status "Accepted".
3. **Future Changes**: When a decision is replaced by a new one, update the status to "Superseded by ADR-XXXX" and create the new ADR.

This approach aligns with the original purpose of ADRs: to record decisions that have been made, not to propose new ones.

### ADR Lifecycle and Status Transitions

The following diagram illustrates the complete ADR lifecycle, including the pre-ADR discussion phase and status transitions:

```mermaid
---
title: ADR Lifecycle and Status Transitions
---
%%{init: {"theme": "neutral", "themeVariables": {"primaryColor": "#4caf50", "primaryTextColor": "#000", "primaryBorderColor": "#2e7d32"}}}%%
stateDiagram-v2
    [*] --> Discussion: Architecture Need Identified

    state "Pre-ADR Phase" as PreADR {
        Discussion --> Proposal: Draft Solution
        Proposal --> Review: Community Review
        Review --> Consensus: Agreement Reached
        Review --> Discussion: Needs Revision
        Consensus --> [*]: Decision Made
    }

    PreADR --> ADR_Created: Create ADR Document

    state "ADR Management" as ADRPhase {
        ADR_Created --> Accepted: Initial Status
        Accepted --> Superseded: Replaced by New ADR
    }

    Superseded --> [*]: End of Lifecycle

    note right of PreADR
        Location: GitHub Issues,
        Pull Requests, Meetings,
        Architecture Channels
    end note

    note right of ADRPhase
        Location: Git Repository
        docs/architecture/adr/
    end note

    note left of Accepted
        All new ADRs start
        with "Accepted" status
    end note
```

#### Phase Descriptions

1. **Pre-ADR Phase** (Outside Git Repository):
   - **Discussion**: Initial problem identification and exploration
   - **Proposal**: Concrete solution proposals are drafted
   - **Review**: Community evaluates proposals through various channels
   - **Consensus**: Agreement reached on the architectural decision

2. **ADR Management Phase** (In Git Repository):
   - **ADR Created**: Document created following the template
   - **Accepted**: Default status for all new ADRs
   - **Superseded**: Decision replaced by a new ADR

#### Key Points

- The "Proposed" status is eliminated from the Git-managed ADR workflow
- All architectural proposals and discussions happen before ADR creation
- ADRs are created only after decisions are made
- Status transitions are one-way (no reverting to previous states)

## Naming Convention

ADRs are named using the following pattern:

```
NNNN-short-decision-title.md
```

Where:

- `NNNN` is a zero-padded sequential number (e.g., 0001, 0002, 0003)
- `short-decision-title` is a kebab-case summary of the decision

## Current ADRs

### ADR Relationship Diagram

The following diagram shows the relationships and dependencies between current ADRs:

```mermaid
---
title: ADR Relationship and Evolution
---
%%{init: {"theme": "neutral", "themeVariables": {"primaryColor": "#4caf50", "primaryTextColor": "#2e7d32", "primaryBorderColor": "#2e7d32"}}}%%
graph TD
    %% Foundation ADRs
    ADR_001[ADR-0001: Local-First Architecture<br/>🏠 Core Architecture]

    %% Standards and Principles
    ADR_002[ADR-0002: AI-Driven Development<br/>🤖 AI Integration]
    ADR_003[ADR-0003: Adopt Industry Standards<br/>📐 Standards-First]

    %% Implementation Approach
    ADR_004[ADR-0004: Diagrams-First Documentation<br/>📊 Visual Communication]
    ADR_005[ADR-0005: CLI-First Interface Architecture<br/>💻 Interface Design]

    %% Process and Organization
    ADR_006[ADR-0006: Adopt Diátaxis Documentation Framework<br/>📋 Documentation Organization]
    ADR_009[ADR-0009: Adopt Apache License 2.0<br/>⚖️ Governance]


    %% Architecture Design
    ADR_007[ADR-0007: Domain-Driven Design<br/>🎯 Domain Modeling]
    ADR_008[ADR-0008: Clean Architecture Adoption<br/>🏗️ Architectural Structure]
    ADR_0010[ADR-0010: Adopt ULID for Distributed Identifiers<br/>🔢 Distributed Systems]

    %% Dependencies
    ADR_003 --> ADR_004
    ADR_003 --> ADR_005
    ADR_001 --> ADR_002
    ADR_003 --> ADR_006
    ADR_004 --> ADR_005
    ADR_001 --> ADR_007
    ADR_003 --> ADR_007
    ADR_007 --> ADR_008
    ADR_002 --> ADR_009
    ADR_003 --> ADR_009
    ADR_001 --> ADR_0010
    ADR_003 --> ADR_0010


    %% Influences (dotted lines)
    ADR_002 -.-> ADR_005
    ADR_001 -.-> ADR_005
    ADR_002 -.-> ADR_007
    ADR_008 -.-> ADR_0010

    %% Styling
    classDef foundation fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
    classDef standards fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
    classDef implementation fill:#fff3e0,stroke:#ff9800,stroke-width:2px
    classDef process fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px
    classDef architecture fill:#fce4ec,stroke:#e91e63,stroke-width:2px
    classDef governance fill:#ede7f6,stroke:#673ab7,stroke-width:2px
    classDef distributed fill:#ffecb3,stroke:#f57f17,stroke-width:2px
    classDef structural fill:#e1f5fe,stroke:#0277bd,stroke-width:2px


    class ADR_001,ADR_002 foundation
    class ADR_003,ADR_004 standards
    class ADR_005 implementation
    class ADR_006 process
    class ADR_007 architecture
    class ADR_008 structural
    class ADR_009 governance
    class ADR_0010 distributed
```

### ADR Index

| ADR | Status | Title | Description |
|-----|--------|-------|-------------|
| [0001](./0001-local-first-architecture.md) | Accepted | Local-First Architecture | Define local-first principles and data storage approach |
| [0002](./0002-ai-driven-development-architecture.md) | Accepted | AI-Driven Development Architecture | Framework for AI assistant integration and collaboration |
| [0003](./0003-adopt-industry-standards.md) | Accepted | Adopt Industry Standards | Prefer established standards over custom implementations |
| [0004](./0004-diagrams-first-documentation.md) | Accepted | Diagrams-First Documentation | Use Mermaid for visual specification documentation |
| [0005](./0005-cli-first-interface-architecture.md) | Accepted | CLI-First Interface Architecture | Command-line interface as foundation for all other interfaces |
| [0006](./0006-adopt-diataxis-documentation-framework.md) | Accepted | Adopt Diátaxis Documentation Framework | Organize documentation using user-centered categories |
| [0007](./0007-domain-driven-design-adoption.md) | Accepted | Domain-Driven Design (DDD) Adoption | Model complex business logic with DDD principles and patterns |
| [0008](./0008-clean-architecture-adoption.md) | Accepted | Clean Architecture Adoption | Implement Clean Architecture patterns with explicit layer separation |
| [0009](./0009-adopt-apache-license-2.0.md) | Accepted | Adopt Apache License 2.0 | Change project license to Apache 2.0 for better legal protection |
| [0010](./0010-adopt-ulid-for-distributed-identifiers.md) | Accepted | Adopt ULID for Distributed Identifiers | Replace hex IDs with ULID for multi-device synchronization support |

#### Legend

- 📋 Process and workflow decisions
- 🏠 Core architectural principles
- 🤖 AI integration and automation
- 📐 Standards and conventions
- 📊 Documentation and communication
- 💻 Interface and user experience
- 🎯 Architecture design patterns
- 🏗️ Architectural structure and organization
- ⚖️ Governance and legal decisions
- 🔢 Distributed systems and data consistency

## Tools and Resources

### Template Usage

To create a new ADR:

1. Copy the [`TEMPLATE.md`](./TEMPLATE.md) file
2. Rename following the naming convention
3. Replace placeholder text with actual content
4. Update the ADR index in this README

## References

- [ADR GitHub Organization](https://adr.github.io/) - Community resources and tools
- [Architecture Decision Records by Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [Architecture Overview](../architecture-overview.md) - Architecture concepts and principles
- [CONTRIBUTING.md](../../../CONTRIBUTING.md) - ADR creation guidelines and workflow integration
