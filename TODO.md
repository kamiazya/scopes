# Scopes Implementation Roadmap

## Overview

This document outlines the implementation roadmap for Scopes, following Clean Architecture and Domain-Driven Design (DDD) principles. We start with the domain layer and application layer to establish a solid foundation for the system.

## Project Vision

**Scopes** is a next-generation local-first task and project management tool designed for symbiotic collaboration between developers and AI assistants. The core innovation is the unified "Scope" entity - a single recursive concept that replaces traditional project/epic/task hierarchies.

## Architecture Approach

- **Clean Architecture**: Clear separation of concerns with dependency rules pointing inward
- **Domain-Driven Design**: Rich domain models encapsulating business logic
- **Local-First**: Offline-capable with selective synchronization
- **AI-Native**: Designed for human-AI collaboration from the ground up

## Implementation Phases

### Phase 1: Core Domain Layer 🚀

#### 1.1 Value Objects
- [ ] `ScopeId`: ULID-based identifier
- [ ] `ScopeTitle`: Title with validation rules
- [ ] `ScopeDescription`: Rich text description
- [ ] `ScopeStatus`: Lifecycle states (pending → in_progress → completed → logged)
- [ ] `Timestamp`: Creation and modification timestamps

#### 1.2 Core Entities
- [ ] `Scope` Entity
      - [ ] Basic properties (id, title, description, status, timestamps)
      - [ ] Parent-child relationships
      - [ ] Factory methods for controlled creation
      - [ ] Business rule enforcement
      - [ ] State transition validation

#### 1.3 Aspect System
- [ ] `AspectKey` and `AspectValue` value objects
- [ ] `Aspect` entity for key-value metadata
- [ ] Built-in aspects:
      - [ ] `priority`: high | medium | low
      - [ ] `status`: pending | in_progress | completed | logged
      - [ ] `type`: feature | bug | task | research | design
- [ ] Custom aspect support

### Phase 2: Domain Services

- [ ] `ScopeHierarchyService`
      - [ ] Parent-child relationship management
      - [ ] Hierarchy validation rules
      - [ ] Tree traversal operations

- [ ] `AspectValidator`
      - [ ] Aspect value validation
      - [ ] Type checking for custom aspects
      - [ ] Constraint enforcement

- [ ] `AspectQueryService`
      - [ ] Query parsing logic
      - [ ] Comparison operators (>=, <=, =, !=)
      - [ ] Logical operators (AND, OR)

### Phase 3: Repository Interfaces

- [ ] `ScopeRepository`
      ```typescript
        - save(scope: Scope): Promise<void>
        - findById(id: ScopeId): Promise<Scope?>
        - findByParent(parentId: ScopeId): Promise<Scope[]>
        - findAll(): Promise<Scope[]>
        - delete(id: ScopeId): Promise<void>
      ```

- [ ] `AspectRepository`
      ```typescript
        - saveAspects(scopeId: ScopeId, aspects: Aspect[]): Promise<void>
        - findByScopeId(scopeId: ScopeId): Promise<Aspect[]>
        - removeAspect(scopeId: ScopeId, key: AspectKey): Promise<void>
      ```

- [ ] Specification Pattern
      - [ ] `ScopeSpecification` interface
      - [ ] `AspectSpecification` for complex queries

### Phase 4: Application Layer (Use Cases)

#### 4.1 Basic Scope Management
- [ ] `CreateScopeUseCase`
      - [ ] Input validation
      - [ ] Scope creation with factory
      - [ ] Repository persistence
      - [ ] Response mapping

- [ ] `UpdateScopeUseCase`
      - [ ] Scope retrieval
      - [ ] Update validation
      - [ ] State transition rules
      - [ ] Persistence

- [ ] `GetScopeUseCase`
      - [ ] Single scope retrieval
      - [ ] Include children option
      - [ ] Include aspects option

- [ ] `ListScopesUseCase`
      - [ ] Pagination support
      - [ ] Basic filtering
      - [ ] Sorting options

- [ ] `DeleteScopeUseCase`
      - [ ] Soft delete implementation
      - [ ] Cascade options for children

#### 4.2 Hierarchy Management
- [ ] `AddChildScopeUseCase`
      - [ ] Parent validation
      - [ ] Hierarchy depth checks
      - [ ] Circular reference prevention

- [ ] `MoveScopeUseCase`
      - [ ] Valid move validation
      - [ ] Update parent references
      - [ ] Maintain child relationships

- [ ] `GetScopeTreeUseCase`
      - [ ] Recursive tree building
      - [ ] Depth limiting
      - [ ] Performance optimization

#### 4.3 Aspect Management
- [ ] `SetAspectUseCase`
      - [ ] Aspect validation
      - [ ] Overwrite vs append logic
      - [ ] Built-in aspect handling

- [ ] `RemoveAspectUseCase`
      - [ ] Aspect removal
      - [ ] Cascade effects

- [ ] `QueryByAspectUseCase`
      - [ ] Query parsing
      - [ ] Complex criteria support
      - [ ] Result mapping

### Phase 5: Context Management

#### 5.1 Workspace Management
- [ ] `Workspace` entity
- [ ] `SetWorkspaceUseCase`
- [ ] `GetWorkspaceUseCase`
- [ ] Directory-based auto-detection

#### 5.2 Focus Management
- [ ] `Focus` value object
- [ ] `SetFocusUseCase`
- [ ] `GetFocusUseCase`
- [ ] Natural language resolution ("this", "that")

#### 5.3 Context Integration
- [ ] `GetCurrentContextUseCase`
- [ ] Context-aware command execution
- [ ] AI integration points

### Phase 6: DTO and Interface Definitions

- [ ] Request/Response DTOs
      - [ ] `CreateScopeRequest/Response`
      - [ ] `UpdateScopeRequest/Response`
      - [ ] `AspectQueryRequest/Response`
      - [ ] `WorkspaceContextDTO`

- [ ] Use Case Interface
      - [ ] `UseCase<TRequest, TResponse>` base interface
      - [ ] Error handling standardization
      - [ ] Validation patterns

## Testing Strategy

### Domain Layer Testing
- Pure unit tests with no external dependencies
- Business rule validation
- Entity state transitions
- Value object equality and immutability

### Application Layer Testing
- Use case workflow testing
- Mocked repository implementations
- Error handling scenarios
- Integration between use cases

### Test Coverage Goals
- Domain Layer: 100% coverage
- Application Layer: 90%+ coverage
- Focus on business-critical paths

## Technical Considerations

### Language Selection (Pending)
- Currently evaluating Kotlin for type safety and domain modeling
- Design patterns are language-agnostic
- Interfaces prepared for dependency injection

### Development Principles
- Interface-based design for flexibility
- Immutability by default for value objects
- Factory methods for complex object creation
- Repository pattern for persistence abstraction

## Milestones

### Milestone 1: Basic Scope Management ✨
- Create, read, update, delete scopes
- Parent-child relationships
- Basic hierarchy navigation
- **Target**: Foundation for all other features

### Milestone 2: Aspect System 🏷️
- Aspect definition and storage
- Query engine implementation
- Complex filtering capabilities
- **Target**: Enable powerful organization

### Milestone 3: Context Management 🎯
- Workspace detection and switching
- Focus management
- Context-aware operations
- **Target**: Seamless developer experience

### Milestone 4: AI Integration 🤖
- Comment-based AI interaction
- MCP server implementation
- Context passing to AI
- **Target**: Human-AI symbiosis

## Next Steps

1. [ ] Review and approve this roadmap
2. [ ] Set up project structure following Clean Architecture
3. [ ] Begin Phase 1 implementation with value objects
4. [ ] Establish testing framework and CI/CD pipeline
5. [ ] Create ADRs for significant decisions during implementation

## Success Criteria

- [ ] All use cases have corresponding user story coverage
- [ ] Domain model accurately represents business requirements
- [ ] Zero coupling between domain and infrastructure layers
- [ ] All features work offline-first
- [ ] AI integration enhances but doesn't require core functionality

---

*This roadmap is a living document and will be updated as implementation progresses and new insights emerge.*

