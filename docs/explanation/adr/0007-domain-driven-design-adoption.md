# ADR-0007: Domain-Driven Design (DDD) Adoption

## Status

Accepted

## Context

The Scopes system needs to handle complex business logic around unified scope management, user workflows, and AI-assisted development processes. As the system grows, we need a robust approach to:

- Model complex domain concepts (unified Scope entities, Workspaces, Focus management, AI integration)
- Maintain business logic consistency across different interfaces (CLI, MCP, future TUI/Web)
- Enable clear communication between domain experts and developers
- Support future extensions with new business rules and processes
- Ensure the core domain remains independent of infrastructure concerns

We start with a single Bounded Context focused on "Scope Management". Future functional expansions (e.g., user management, analytics, reporting) may be designed as separate Bounded Contexts to maintain clear boundaries and prevent concept pollution.

Traditional approaches like anemic domain models or transaction script patterns would lead to:

- Business logic scattered across service layers
- Difficulty in testing complex business scenarios
- Tight coupling between domain concepts and infrastructure
- Reduced ability to respond to changing requirements

## Decision

We will adopt Domain-Driven Design (DDD) as our primary approach for modeling the task management domain.

### Core DDD Elements to Adopt

1. **Entities**: Rich domain objects with identity and lifecycle
  - Encapsulate business rules and enforce invariants
  - Use static factory methods for controlled instance creation
  - Maintain clear identity and lifecycle management

2. **Value Objects**: Immutable objects without identity
  - Replace primitive obsession with domain-specific types
  - Enforce immutability through language features
  - Encapsulate validation and business constraints

3. **Domain Services**: Business logic that doesn't naturally fit in entities
  - Handle cross-entity operations and calculations
  - Maintain stateless behavior focused on domain logic
  - Keep infrastructure concerns separate

4. **Repositories**: Abstractions for data persistence
  - Define domain-focused persistence operations
  - Handle mapping between domain model and persistence format
  - Shield domain layer from infrastructure details

5. **Use Cases (Application Services)**: Orchestration of domain operations
  - Coordinate domain objects to fulfill business scenarios
  - Maintain clear separation from delivery mechanisms (UI, API, CLI)
  - Handle transaction boundaries and cross-cutting concerns

## Options Considered

### Option 1: Anemic Domain Model

- **Pros**: Simpler initial implementation, familiar to many developers
- **Cons**: Business logic scattered, poor encapsulation, testing difficulties
- **Rejected**: Would lead to maintenance issues as complexity grows

### Option 2: Transaction Script Pattern

- **Pros**: Straightforward for simple CRUD operations
- **Cons**: No domain modeling, logic duplication, poor scalability
- **Rejected**: Insufficient for complex business scenarios

### Option 3: Full DDD with Complex Aggregates

- **Pros**: Complete domain modeling capabilities
- **Cons**: High complexity, potential over-engineering for current scope
- **Modified**: Adopted simplified DDD approach suitable for current requirements

### Option 4: Event-Driven Architecture

- **Pros**: Excellent for distributed systems, audit trails
- **Cons**: Additional complexity, event store requirements
- **Deferred**: May be adopted later for audit functionality

## Consequences

### Positive

- **Clear Business Logic**: Domain concepts are explicitly modeled and encapsulated
- **Testability**: Business rules can be tested independently of infrastructure
- **Maintainability**: Changes to business logic are localized within domain entities
- **Communication**: Domain model serves as shared language between developers and domain experts
- **Extensibility**: New business rules can be added without affecting infrastructure layers

### Negative

- **Learning Curve**: Team members need to understand DDD concepts and patterns
- **Initial Complexity**: More sophisticated than simple CRUD operations
- **Potential Over-Engineering**: Risk of creating unnecessary abstractions
  - Mitigation: Start with simple value objects and entities, introduce aggregates only when transaction consistency becomes necessary

### Neutral

- **File Organization**: Domain concepts are organized in `packages/core/src/entities/` and `packages/core/src/usecases/`
- **Testing Strategy**: Domain logic tested separately from infrastructure concerns
- **Future Evolution**: Foundation established for more complex domain modeling as requirements grow

## Implementation Guidelines

### Starting Simple

Begin with fundamental DDD building blocks:

- Rich entities with encapsulated business rules
- Value objects for domain concepts
- Repository abstractions for persistence
- Use cases for application orchestration

### Aggregate Introduction Guidelines

Introduce aggregates when:

- Multiple entities must maintain transactional consistency
- Business invariants span across entity boundaries
- Complex lifecycle management is required
Otherwise, treat entities as independent aggregate roots

### Repository Responsibilities

Repositories should:

- Provide domain-centric persistence abstractions
- Handle bidirectional mapping between domain and storage models
- Isolate domain layer from persistence technology
- Support both creation and reconstitution workflows

### Evolution Path

As the system grows, consider introducing:

- Aggregate patterns for complex consistency boundaries
- Domain events for decoupled communication
- Specification pattern for reusable business rules
- Integration with architectural patterns (e.g., event-driven, CQRS)

## References

- [Domain-Driven Design by Eric Evans](https://domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://vaughnvernon.co/?page_id=168)
- [Clean Architecture by Robert Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
