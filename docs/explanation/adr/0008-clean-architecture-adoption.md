# ADR-0008: Clean Architecture Adoption

## Status

Accepted

## Context

The Scopes system has successfully adopted Domain-Driven Design (DDD) principles as defined in ADR-0007. While DDD provides excellent domain modeling capabilities, we need a complementary architectural pattern to:

- **Clarify dependency relationships** between different layers of the system
- **Enforce architectural boundaries** to prevent coupling between infrastructure and domain logic
- **Support multiple interfaces** (CLI, MCP Server, future TUI/Web) with consistent access to business logic
- **Enhance testability** by enabling isolated testing of each architectural layer
- **Facilitate future expansion** with clear guidelines for adding new components

The current architecture demonstrates good separation of concerns between domain logic, application services, and infrastructure implementations. However, the relationships between these layers and the dependency flow are not explicitly defined, which could lead to:

- **Accidental coupling** between layers
- **Inconsistent dependency directions** causing circular dependencies
- **Difficulty in testing** isolated components
- **Unclear boundaries** when adding new features

## Decision

We will adopt Clean Architecture principles to complement our existing Domain-Driven Design implementation. This decision establishes explicit architectural layers with clear dependency rules.

### Clean Architecture Layer Structure

```mermaid
---
title: Clean Architecture Layers
---
%%{init: {"theme": "neutral", "themeVariables": {"primaryColor": "#4caf50", "primaryTextColor": "#2e7d32", "primaryBorderColor": "#2e7d32"}}}%%
graph TB
    subgraph "Frameworks & Drivers"
        UI[User Interfaces]
        EXT[External Interfaces]
        DB[Database/Storage]
        WEB[Web Framework]
    end

    subgraph "Interface Adapters"
        CTRL[Controllers]
        GATE[Gateways]
        PRES[Presenters]
        REPO_IMPL[Repository Implementations]
    end

    subgraph "Use Cases"
        APP[Application Services]
        REPO_INT[Repository Interfaces]
        PORTS[Ports/Boundaries]
    end

    subgraph "Entities"
        ENT[Domain Entities]
        VO[Value Objects]
        DS[Domain Services]
        DR[Domain Rules]
    end

    %% Dependencies flow inward
    UI --> CTRL
    EXT --> GATE
    DB --> REPO_IMPL
    WEB --> PRES

    CTRL --> APP
    GATE --> APP
    PRES --> APP
    REPO_IMPL --> REPO_INT

    APP --> ENT
    APP --> VO
    APP --> DS
    REPO_INT --> ENT
    PORTS --> ENT

    %% Styling
    classDef outer fill:#ffcdd2,stroke:#d32f2f,stroke-width:2px
    classDef adapter fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef usecase fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
    classDef entity fill:#e3f2fd,stroke:#2196f3,stroke-width:3px

    class UI,EXT,DB,WEB outer
    class CTRL,GATE,PRES,REPO_IMPL adapter
    class APP,REPO_INT,PORTS usecase
    class ENT,VO,DS,DR entity
```

### Dependency Rules

1. **Dependency Inversion**: Dependencies point inward only
2. **Interface Segregation**: Outer layers depend on inner layer interfaces
3. **Stable Dependencies**: Inner layers are more stable than outer layers
4. **Framework Independence**: Business logic independent of external frameworks

### Integration with DDD

Clean Architecture layers align with DDD concepts:

- **Entities Layer**: Domain entities, value objects, domain services
- **Use Cases Layer**: Application services, repository interfaces
- **Interface Adapters Layer**: Repository implementations, external service adapters
- **Frameworks & Drivers Layer**: User interfaces, external APIs, persistence mechanisms

## Consequences

### Positive

- **Clear Architectural Boundaries**: Explicit layer responsibilities and dependency rules
- **Enhanced Testability**: Each layer can be tested independently with appropriate test doubles
- **Framework Independence**: Business logic remains isolated from external dependencies
- **Scalability**: New interfaces (TUI, Web, API) can be added without affecting business logic
- **Consistency**: Uniform approach to adding new features across all layers
- **Maintainability**: Changes in outer layers don't affect inner layers

### Negative

- **Increased Complexity**: Additional abstractions and interfaces required
- **Learning Curve**: Team must understand Clean Architecture principles
- **Initial Overhead**: More code required for proper layering and abstractions
- **Potential Over-Engineering**: Risk of excessive abstraction for simple operations

### Neutral

- **Code Organization**: Requires reorganization of some existing code to align with layers
- **Testing Strategy**: May require updates to testing approach for layer isolation
- **Documentation**: Additional documentation needed for architectural guidelines

## Alternatives Considered

### Alternative 1: Layered Architecture (Traditional N-Tier)

- **Pros**: Simpler to understand, familiar to most developers
- **Cons**: Allows coupling between layers, difficult to test, framework-dependent
- **Rejected**: Would undermine the benefits of our DDD implementation

### Alternative 2: Hexagonal Architecture (Ports and Adapters)

- **Pros**: Clear separation of core and infrastructure, testable
- **Cons**: Less prescriptive about internal organization, could be confusing with DDD
- **Partially Adopted**: Clean Architecture incorporates hexagonal architecture concepts

### Alternative 3: Onion Architecture

- **Pros**: Similar benefits to Clean Architecture, domain-centric
- **Cons**: Less mature ecosystem, fewer established patterns
- **Rejected**: Clean Architecture provides better guidance and community support

### Alternative 4: Maintain Current Structure Without Formalization

- **Pros**: No additional work required, existing structure mostly correct
- **Cons**: Risk of architectural drift, unclear boundaries for new developers
- **Rejected**: Formalization provides long-term maintainability benefits

## Related Decisions

- **Built Upon**: [ADR-0007: Domain-Driven Design Adoption](./0007-domain-driven-design-adoption.md) - Clean Architecture provides the structural framework for DDD implementation
- **Influences**: [ADR-0005: CLI-First Interface Architecture](./0005-cli-first-interface-architecture.md) - CLI remains the primary interface but within Clear Architecture's outer layer
- **Supports**: [ADR-0001: Local-First Architecture](./0001-local-first-architecture.md) - Clean Architecture enables local-first design through dependency inversion

## Scope

### Bounded Context

- **Primary**: Scope Management Context
- **Future**: Additional contexts as system grows

### Components

- **Core Domain**: Domain entities, value objects, and domain services
- **Application Layer**: Use cases and application services
- **Infrastructure**: Repository implementations, external service adapters
- **Presentation**: User interfaces and external API endpoints

### External Systems

- **Local Storage**: File-based persistence
- **External APIs**: Project management tool integrations
- **AI Systems**: AI assistant integration endpoints

## Tags

`architecture`, `domain-design`, `clean-architecture`, `dependency-injection`, `testing`, `maintainability`
