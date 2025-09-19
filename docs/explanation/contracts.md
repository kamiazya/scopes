# Contracts Layer

## Overview

The Contracts layer provides stable, well-defined interfaces between bounded contexts in the Scopes system. It acts as an explicit boundary that enables contexts to evolve independently while maintaining clear integration points.

## Purpose and Role

```mermaid
graph TB
    subgraph "Contracts Layer Benefits"
        DC[Decoupling<br/>Bounded Contexts]
        SA[Stable API<br/>Contracts]
        ET[Error<br/>Translation]
        TS[Type<br/>Safety]
        DOC[Living<br/>Documentation]
    end

    subgraph "Key Features"
        IF[Interface<br/>Definitions]
        ER[Error<br/>Mapping]
        VER[Versioning<br/>Support]
        INT[Integration<br/>Points]
    end

    DC --> IF
    SA --> VER
    ET --> ER
    TS --> IF
    DOC --> INT

    classDef benefit fill:#e8f5e9,stroke:#4caf50
    classDef feature fill:#e3f2fd,stroke:#2196f3

    class DC,SA,ET,TS,DOC benefit
    class IF,ER,VER,INT feature
```

The Contracts layer serves several critical purposes:

1. **Decoupling Bounded Contexts**: Provides explicit interfaces that prevent direct dependencies between contexts
2. **Stable API Contracts**: Defines stable interfaces that can be versioned independently
3. **Error Translation**: Maps domain-specific errors to contract-level errors that consumers can understand
4. **Type Safety**: Provides strongly-typed commands, queries, and results for inter-context communication
5. **Documentation**: Serves as living documentation of context capabilities and integration points

## Architecture Position

```mermaid
graph TB
    subgraph "System Architecture"
        subgraph "Apps Layer"
            CLI[CLI Application]
            DAEMON[Daemon Service]
        end

        subgraph "Interfaces Layer"
            FACADE[Facades & Adapters]
        end

        subgraph "Contracts Layer"
            SMP[ScopeManagementPort]
            UPP[UserPreferencesPort]
            WMP[WorkspaceManagementPort]
        end

        subgraph "Bounded Contexts"
            subgraph "Scope Management"
                SM_APP[Application Layer]
                SM_INFRA[Infrastructure Layer]
            end

            subgraph "User Preferences"
                UP_APP[Application Layer]
            end
        end
    end

    CLI --> FACADE
    DAEMON --> FACADE

    FACADE --> SMP
    FACADE --> UPP

    SMP --> SM_APP
    UPP --> UP_APP

    SM_INFRA --> UPP

    classDef contracts fill:#ffe3ba,stroke:#333,stroke-width:3px
    classDef app fill:#e1f5fe,stroke:#01579b
    classDef interface fill:#e8f5e9,stroke:#2e7d32
    classDef context fill:#fff3e0,stroke:#e65100

    class SMP,UPP,WMP contracts
    class CLI,DAEMON app
    class FACADE interface
    class SM_APP,SM_INFRA,UP_APP context
```

## Port Interface Pattern

All port interfaces follow a consistent pattern for reliability and type safety:

```mermaid
sequenceDiagram
    participant Client
    participant Port
    participant Handler
    participant Domain

    Client->>Port: executeCommand(command)
    activate Port

    Port->>Handler: process(command)
    activate Handler

    Handler->>Domain: execute business logic
    Domain-->>Handler: Either<DomainError, Result>

    Handler->>Port: map errors & results
    deactivate Handler

    Port-->>Client: Either<ContractError, ContractResult>
    deactivate Port

    Note over Client,Domain: All operations use Either for explicit error handling
```

### Port Interface Template

```kotlin
interface SomeContextPort {
    // Commands return Either<Error, Result>
    suspend fun executeCommand(command: Command): Either<ContractError, CommandResult>

    // Queries return Either<Error, Result?>
    suspend fun executeQuery(query: Query): Either<ContractError, QueryResult?>
}
```

### Key Characteristics

1. **Suspend Functions**: All operations are suspending for async support
2. **Either Return Type**: Explicit error handling without exceptions
3. **Contract-Level Types**: No domain types exposed in interfaces
4. **Null Safety**: Queries return nullable results for "not found" cases

## Bounded Context Coordination Patterns

### Context Relationship Types

```mermaid
graph LR
    subgraph "Coordination Patterns"
        subgraph "Customer-Supplier"
            UP[User Preferences<br/>Supplier] --> |provides settings| SM[Scope Management<br/>Customer]
            UP --> |provides settings| WM[Workspace Management<br/>Customer]
        end

        subgraph "Partnership"
            SM <--> |mutual dependency| WM
        end

        subgraph "Shared Kernel"
            P[Platform<br/>Commons] --> SM
            P --> UP
            P --> WM
        end
    end

    classDef supplier fill:#e8f5e9,stroke:#4caf50
    classDef customer fill:#e3f2fd,stroke:#2196f3
    classDef partnership fill:#fff3e0,stroke:#ff9800
    classDef shared fill:#f5f5f5,stroke:#9e9e9e

    class UP supplier
    class SM,WM customer
    class P shared
```

### Integration Adapter Pattern

```mermaid
graph TB
    subgraph "Cross-Context Integration"
        subgraph "Scope Management Context"
            SMD[Domain Layer]
            SMA[Application Layer]
            SMI[Infrastructure Layer]
        end

        subgraph "User Preferences Context"
            UPC[UserPreferencesPort<br/>Contract Interface]
        end

        subgraph "Integration Adapter"
            ADAPTER[UserPreferencesToHierarchyPolicyAdapter]
            ERROR_MAP[Error Mapper]
        end
    end

    SMI --> ADAPTER
    ADAPTER --> UPC
    ADAPTER --> ERROR_MAP
    ERROR_MAP --> SMA

    ADAPTER -.->|translates| HPP[HierarchyPolicyProvider<br/>Domain Interface]

    classDef domain fill:#e8f5e9,stroke:#4caf50
    classDef contract fill:#ffe3ba,stroke:#ff6f00
    classDef adapter fill:#e3f2fd,stroke:#2196f3

    class SMD,SMA,SMI,HPP domain
    class UPC contract
    class ADAPTER,ERROR_MAP adapter
```

**Adapter Responsibilities:**
- Translate between contract types and domain types
- Map contract errors to domain errors
- Handle null/missing data appropriately
- Provide fallback behavior when external context is unavailable

## Error Mapping Strategy

### Error Hierarchy Design

```mermaid
graph TB
    subgraph "Contract Error Hierarchy"
        CE[ContractError<br/>Base Interface]

        CE --> IE[InputError<br/>Client input problems]
        CE --> BE[BusinessError<br/>Business rule violations]
        CE --> SE[SystemError<br/>Infrastructure issues]

        IE --> IT[InvalidTitle]
        IE --> IF[InvalidFormat]
        IE --> MF[MissingField]

        BE --> NF[NotFound]
        BE --> AC[AccessDenied]
        BE --> RV[RuleViolation]

        SE --> SU[ServiceUnavailable]
        SE --> TO[Timeout]
        SE --> IC[InternalError]
    end

    classDef base fill:#f5f5f5,stroke:#9e9e9e,stroke-width:3px
    classDef category fill:#e3f2fd,stroke:#2196f3
    classDef specific fill:#fff3e0,stroke:#ff9800

    class CE base
    class IE,BE,SE category
    class IT,IF,MF,NF,AC,RV,SU,TO,IC specific
```

### Error Translation Flow

```mermaid
sequenceDiagram
    participant Domain
    participant ErrorMapper
    participant Contract
    participant Client

    Domain->>ErrorMapper: DomainError
    activate ErrorMapper

    ErrorMapper->>ErrorMapper: Categorize error type
    ErrorMapper->>ErrorMapper: Extract relevant context
    ErrorMapper->>ErrorMapper: Create contract error

    ErrorMapper->>Contract: ContractError
    deactivate ErrorMapper

    Contract->>Client: Either.Left(ContractError)

    Note over Domain,Client: Preserves essential information while hiding implementation details
```

### Mapping Principles

1. **Preserve Context**: Include relevant information from domain errors
2. **Hide Implementation**: Don't expose internal types or structures
3. **Categorize Appropriately**: Map to the correct error category (Input/Business/System)
4. **Provide Clear Messages**: Ensure error messages are understandable to consumers

## Contract Definition Structure

### Commands and Queries

```mermaid
graph LR
    subgraph "CQRS in Contracts"
        subgraph "Commands"
            CC[CreateScopeCommand]
            UC[UpdateScopeCommand]
            DC[DeleteScopeCommand]
        end

        subgraph "Queries"
            GQ[GetScopeQuery]
            LQ[ListScopesQuery]
            SQ[SearchScopesQuery]
        end

        subgraph "Results"
            CR[CreateScopeResult]
            SR[ScopeResult]
            LR[ScopeListResult]
        end
    end

    CC --> CR
    UC --> SR
    DC --> SR
    GQ --> SR
    LQ --> LR
    SQ --> LR

    classDef command fill:#ffe0e0,stroke:#d32f2f
    classDef query fill:#e0f0ff,stroke:#1976d2
    classDef result fill:#e8f5e9,stroke:#388e3c

    class CC,UC,DC command
    class GQ,LQ,SQ query
    class CR,SR,LR result
```

### Type Safety Requirements

| Component | Requirements | Examples |
|-----------|-------------|----------|
| **Commands** | Immutable data classes | `CreateScopeCommand(title, description)` |
| **Queries** | Parameter objects | `GetScopeQuery(scopeId)` |
| **Results** | Serializable DTOs | `ScopeResult(id, title, createdAt)` |
| **Errors** | Sealed hierarchies | `InputError`, `BusinessError`, `SystemError` |

## Integration Examples

### Port Implementation Pattern

```kotlin
// Contract interface
interface ScopeManagementPort {
    suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult>
}

// Infrastructure adapter implementation
class ScopeManagementPortAdapter(
    private val createScopeHandler: CreateScopeHandler,
    private val errorMapper: ApplicationErrorMapper
) : ScopeManagementPort {

    override suspend fun createScope(command: CreateScopeCommand) =
        createScopeHandler(command.toDomainCommand())
            .mapLeft { error -> errorMapper.map(error) }
            .map { result -> result.toContractResult() }
}
```

### Cross-Context Communication

```mermaid
sequenceDiagram
    participant Facade
    participant ScopePort
    participant UserPrefPort
    participant ScopeContext
    participant UserPrefContext

    Facade->>UserPrefPort: getHierarchyPrefs()
    UserPrefPort->>UserPrefContext: query preferences
    UserPrefContext-->>UserPrefPort: Either<Error, Preferences>
    UserPrefPort-->>Facade: Contract result

    Facade->>ScopePort: createScope(command, preferences)
    ScopePort->>ScopeContext: execute with preferences
    ScopeContext-->>ScopePort: Either<Error, Scope>
    ScopePort-->>Facade: Contract result

    Note over Facade,UserPrefContext: Contexts remain decoupled through contracts
```

## Versioning Strategy

### Contract Evolution

```mermaid
graph TB
    subgraph "Contract Versioning"
        V1[Contract v1.0<br/>Initial API]
        V2[Contract v2.0<br/>Added features]
        V3[Contract v2.1<br/>Backward compatible]

        V1 --> |Breaking changes| V2
        V2 --> |Additive changes| V3
    end

    subgraph "Implementation"
        I1[Implementation v1.0]
        I2[Implementation v2.0]
        I3[Implementation v2.1]
    end

    V1 -.-> I1
    V2 -.-> I2
    V3 -.-> I3

    classDef contract fill:#ffe3ba,stroke:#ff6f00
    classDef impl fill:#e3f2fd,stroke:#2196f3

    class V1,V2,V3 contract
    class I1,I2,I3 impl
```

### Compatibility Guidelines

1. **Additive Changes**: New optional fields, new methods with defaults
2. **Backward Compatible**: Maintain existing method signatures
3. **Breaking Changes**: Require major version increment
4. **Deprecation**: Mark old methods as deprecated before removal

## Testing Contracts

### Contract Compliance Testing

```kotlin
// Test that implementations fulfill contract requirements
class ScopeManagementPortComplianceTest {
    @Test
    fun `should handle all error cases defined in contract`() {
        // Test all contract error scenarios
    }

    @Test
    fun `should return proper types as defined in contract`() {
        // Verify return type compliance
    }
}
```

### Cross-Context Integration Testing

```kotlin
class CrossContextIntegrationTest {
    @Test
    fun `scope creation should integrate with user preferences`() {
        // Test actual cross-context communication
        // through contract interfaces
    }
}
```

## Benefits

### Development Benefits
- **Independent Evolution**: Contexts can evolve without breaking others
- **Clear Boundaries**: Explicit integration points reduce coupling
- **Type Safety**: Compile-time verification of inter-context communication
- **Testing**: Contract interfaces enable easy mocking and testing

### Operational Benefits
- **Monitoring**: Contract calls can be monitored and measured
- **Debugging**: Clear boundaries make issues easier to isolate
- **Versioning**: Independent deployment and versioning of contexts
- **Documentation**: Self-documenting integration points

## Related Documentation

- [Clean Architecture](./clean-architecture.md) - Overall architecture principles
- [Domain-Driven Design](./domain-driven-design.md) - Bounded context concepts
- [Error Handling Guidelines](../guides/development/error-handling.md) - Error handling patterns
