# Alias System API Reference

This document provides a conceptual overview and interface reference for the alias system in Scopes, focusing on core concepts and usage patterns.

## Overview

The alias system provides human-readable identifiers for scopes, enabling intuitive access and management without exposing internal ULIDs.

```mermaid
graph TB
    subgraph "Alias System Architecture"
        subgraph "Domain Layer"
            AN[AliasName<br/>Value Object]
            SA[ScopeAlias<br/>Entity]
            AGS[AliasGenerationService<br/>Interface]
        end

        subgraph "Application Layer"
            AAH[AddAliasHandler]
            RAH[RemoveAliasHandler]
            SCAH[SetCanonicalAliasHandler]
            ARH[AliasResolutionHandler]
        end

        subgraph "Infrastructure Layer"
            AR[AliasRepository<br/>Implementation]
            AGI[AliasGenerationService<br/>Implementation]
            WP[WordProvider<br/>Implementations]
        end
    end

    User[User] --> AAH
    AAH --> SA
    SA --> AN
    AAH --> AR
    AGS --> WP
    AGI --> WP

    classDef domain fill:#e8f5e9,stroke:#4caf50
    classDef application fill:#e3f2fd,stroke:#2196f3
    classDef infrastructure fill:#fce4ec,stroke:#e91e63

    class AN,SA,AGS domain
    class AAH,RAH,SCAH,ARH application
    class AR,AGI,WP infrastructure
```

## Core Concepts

### Alias Types

```mermaid
graph LR
    subgraph "Alias Hierarchy"
        S[Scope] --> CA[Canonical Alias<br/>One per scope]
        S --> CUA[Custom Aliases<br/>Multiple allowed]

        CA --> |Auto-generated| AG[quiet-river-x7k]
        CA --> |User-provided| UP[auth-system]

        CUA --> |Additional refs| A1[sprint-42]
        CUA --> |Team naming| A2[backend-auth]
        CUA --> |Version refs| A3[v2-system]
    end

    classDef canonical fill:#e8f5e9,stroke:#4caf50,stroke-width:3px
    classDef custom fill:#fff3e0,stroke:#ff9800

    class CA,AG,UP canonical
    class CUA,A1,A2,A3 custom
```

### Validation Rules

| Component | Rule | Example |
|-----------|------|---------|
| **Length** | 2-64 characters | `auth` ✅, `a` ❌ |
| **Pattern** | `^[a-z][a-z0-9-_]{1,63}$` | `auth-v2` ✅, `2auth` ❌ |
| **Normalization** | Lowercase | `AUTH` → `auth` |
| **Uniqueness** | Across all scopes | Each alias maps to one scope |

## Command Operations

### Add Alias
```kotlin
// Command pattern
data class AddAliasCommand(
    val scopeAlias: String,
    val newAlias: String
)

// Result
sealed class AddAliasResult {
    data class Success(val aliasAdded: String) : AddAliasResult()
    sealed class Error : AddAliasResult() {
        object AliasAlreadyExists : Error()
        object InvalidAliasFormat : Error()
        object ScopeNotFound : Error()
    }
}
```

### Set Canonical Alias
```kotlin
data class SetCanonicalAliasCommand(
    val currentAlias: String,
    val newCanonicalAlias: String
)
```

### Remove Alias
```kotlin
data class RemoveAliasCommand(
    val alias: String
)
```

## Query Operations

### Alias Resolution Flow

```mermaid
sequenceDiagram
    participant Client
    participant Handler
    participant Repository
    participant ValidationService

    Client->>Handler: resolveAlias("auth")
    Handler->>ValidationService: validateFormat("auth")
    ValidationService-->>Handler: Valid
    Handler->>Repository: findByAlias("auth")
    Repository-->>Handler: ScopeAlias(scopeId, "auth")
    Handler-->>Client: ScopeResult(id, title, ...)

    Note over Client,ValidationService: Supports prefix matching when unique
```

### List Operations
```kotlin
// List all aliases for scope
data class ListAliasesQuery(val scopeAlias: String)

// Search aliases by prefix
data class SearchAliasesQuery(val prefix: String)
```

## Generation Strategies

### Canonical Alias Generation

```mermaid
graph LR
    subgraph "Generation Pipeline"
        Input[ULID Seed] --> Strategy[Generation Strategy]
        Strategy --> Adjective[Adjective Pool]
        Strategy --> Noun[Noun Pool]
        Strategy --> Token[Random Token]

        Adjective --> Combine[Combine Components]
        Noun --> Combine
        Token --> Combine

        Combine --> Validate[Format Validation]
        Validate --> Unique[Uniqueness Check]
        Unique --> Result[Generated Alias]
    end

    classDef process fill:#e3f2fd,stroke:#2196f3
    classDef data fill:#fff3e0,stroke:#ff9800
    classDef result fill:#e8f5e9,stroke:#4caf50

    class Strategy,Combine,Validate,Unique process
    class Adjective,Noun,Token data
    class Result result
```

**Generation Pattern**: `{adjective}-{noun}-{token}`
- **Adjectives**: quiet, brave, swift, gentle, wise...
- **Nouns**: river, mountain, ocean, star, cloud...
- **Tokens**: 3-character alphanumeric (x7k, b2m, c9p...)

### Strategy Implementations

| Strategy | Pattern | Use Case |
|----------|---------|----------|
| **Haikunator** | adjective-noun-token | Default canonical aliases |
| **Custom** | User-defined | Manual alias creation |
| **Incremental** | base-name-{number} | Conflict resolution |

## Error Handling

### Validation Errors
```mermaid
graph TB
    subgraph "Alias Validation Errors"
        VE[ValidationError]
        VE --> TS[TooShort<br/>min: 2 chars]
        VE --> TL[TooLong<br/>max: 64 chars]
        VE --> IF[InvalidFormat<br/>pattern mismatch]
        VE --> IC[InvalidCharacters<br/>non-alphanumeric]
        VE --> CC[ConsecutiveSpecial<br/>-- or __]
    end

    classDef error fill:#ffebee,stroke:#f44336
    class VE,TS,TL,IF,IC,CC error
```

### Business Rule Errors
- **AliasAlreadyExists**: Alias is already assigned to another scope
- **CanonicalAliasRequired**: Cannot remove the last alias from a scope
- **ScopeNotFound**: Referenced scope does not exist

## Integration Points

### Repository Interface
```kotlin
interface AliasRepository {
    suspend fun save(alias: ScopeAlias): Either<Error, Unit>
    suspend fun findByName(name: AliasName): Either<Error, ScopeAlias?>
    suspend fun findByScopeId(scopeId: ScopeId): Either<Error, List<ScopeAlias>>
    suspend fun delete(aliasId: AliasId): Either<Error, Unit>
    suspend fun existsByName(name: AliasName): Either<Error, Boolean>
}
```

### Event Publishing
```kotlin
// Domain events for alias operations
sealed class AliasEvent : DomainEvent {
    data class AliasAdded(val scopeId: ScopeId, val aliasName: AliasName) : AliasEvent()
    data class AliasRemoved(val scopeId: ScopeId, val aliasName: AliasName) : AliasEvent()
    data class CanonicalAliasChanged(val scopeId: ScopeId, val oldAlias: AliasName, val newAlias: AliasName) : AliasEvent()
}
```

## Usage Examples

### CLI Integration
```bash
# Add custom alias
scopes alias add quiet-river-x7k auth-system

# Set canonical alias
scopes alias set-canonical quiet-river-x7k authentication

# Remove alias
scopes alias rm auth-system

# List aliases for scope
scopes alias list authentication

# Search by prefix
scopes alias search auth
```

### MCP Tool Integration
The alias system is exposed through MCP tools:
- `aliases.add` - Add new alias
- `aliases.remove` - Remove alias
- `aliases.setCanonical` - Change canonical alias
- `aliases.list` - List scope aliases
- `aliases.resolve` - Resolve alias to scope

## Performance Considerations

### Indexing Strategy
- **Primary Index**: `alias_name` (unique)
- **Secondary Index**: `scope_id` (for listing scope aliases)
- **Search Optimization**: Prefix matching with LIKE queries

### Caching
- **Alias Resolution**: In-memory cache for frequently accessed aliases
- **Generation**: Cache word pools to avoid repeated file reads
- **Validation**: Cache regex patterns for format validation

## Related Documentation

- [Alias System Architecture](../../explanation/alias-system-architecture.md) - Design concepts
- [CLI Quick Reference](../cli-quick-reference.md) - Command examples
- [MCP Implementation Guide](../mcp-implementation-guide.md) - AI integration
