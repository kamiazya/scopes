# DTO Naming and Placement Guidelines

This guide covers Data Transfer Object (DTO) conventions, naming patterns, and architectural placement within the Scopes project.

## Table of Contents
- [Overview](#overview)
- [Naming Conventions](#naming-conventions)
- [DTO Placement Structure](#dto-placement-structure)
- [Design Principles](#design-principles)
- [Mapping Between Layers](#mapping-between-layers)
- [Migration Strategy](#migration-strategy)
- [Architecture Testing](#architecture-testing)

## Overview

DTOs serve as data structures for transferring information between architectural layers. The project uses distinct naming conventions to maintain clear separation of concerns.

## Naming Conventions

### Application Layer DTOs

**Convention**: Use `Dto` suffix for data structures within application boundaries

```kotlin
// ✅ Application Layer DTO examples
data class ScopeDto(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String?,
    val customAliases: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val aspects: Map<String, List<String>> = emptyMap(),
)

data class CreateScopeResult(
    val scope: ScopeDto,
    val generatedAlias: String?
)
```

**Types**:
- **Query Results**: `ScopeDto`, `AliasDto`, `ContextViewDto`
- **Command Inputs**: `CreateScopeInput`, `UpdateScopeInput`
- **Operation Results**: `CreateScopeResult`, `FilteredScopesResult`

### Contract Layer DTOs

**Convention**: Use semantic suffixes based on purpose

```kotlin
// ✅ Contract Layer DTO examples
data class ScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isArchived: Boolean = false,
    val aspects: Map<String, List<String>> = emptyMap(),
)

data class CreateScopeCommand(
    val title: String,
    val description: String? = null,
    val parentId: String? = null,
    val generateAlias: Boolean = true,
    val customAlias: String? = null,
)
```

**Types**:
- **Query Results**: `Result` suffix (e.g., `ScopeResult`, `AliasListResult`)
- **Commands**: `Command` suffix (e.g., `CreateScopeCommand`, `UpdateScopeCommand`)
- **Queries**: `Query` suffix (e.g., `GetScopeQuery`, `ListAliasesQuery`)

## DTO Placement Structure

### Application Layer Structure

```
contexts/{context}/application/src/main/kotlin/.../application/dto/
├── common/
│   ├── DTO.kt                    # Base DTO marker interface
│   └── PagedResult.kt            # Common pagination wrapper
├── scope/
│   ├── ScopeDto.kt              # Main scope data transfer object
│   ├── CreateScopeResult.kt     # Scope creation response
│   ├── FilteredScopesResult.kt  # Scope filtering response
│   └── UpdateScopeInput.kt      # Scope update input
├── alias/
│   ├── AliasDto.kt              # Alias information
│   ├── AliasListDto.kt          # Alias collection wrapper  
│   └── AliasInfoDto.kt          # Detailed alias metadata
├── context/
│   └── ContextViewDto.kt        # Context view information
└── aspect/
    ├── AspectDefinitionDto.kt   # Aspect definition data
    └── ValidateAspectValueRequest.kt # Aspect validation input
```

### Contract Layer Structure

```
contracts/{context}/src/main/kotlin/.../contracts/{context}/
├── commands/
│   ├── CreateScopeCommand.kt
│   ├── UpdateScopeCommand.kt
│   ├── AddAliasCommand.kt
│   └── RemoveAliasCommand.kt
├── queries/
│   ├── GetScopeQuery.kt
│   ├── ListScopesWithQueryQuery.kt
│   └── GetChildrenQuery.kt
├── results/
│   ├── ScopeResult.kt
│   ├── ScopeListResult.kt
│   ├── AliasInfo.kt
│   └── CreateScopeResult.kt
└── errors/
    └── ScopeContractError.kt
```

## Design Principles

### 1. Layer-Appropriate Design

**Application DTOs** - Rich internal representation:
```kotlin
// ✅ Application DTO - Rich internal representation
data class ScopeDto(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String? = null,          // Optional for internal use
    val customAliases: List<String> = emptyList(), // Rich alias information
    val createdAt: Instant,
    val updatedAt: Instant,
    val aspects: Map<String, List<String>> = emptyMap() // Complex nested data
)
```

**Contract DTOs** - Minimal stable external representation:
```kotlin
// ✅ Contract DTO - Minimal stable external representation  
data class ScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,                  // Required for external consumers
    val createdAt: Instant,
    val updatedAt: Instant,
    val isArchived: Boolean = false,             // Simple boolean flag
    val aspects: Map<String, List<String>> = emptyMap()
)
```

### 2. Immutability and Data Classes

```kotlin
// ✅ Proper DTO structure
data class ScopeDto(
    val id: String,
    val title: String,
    val description: String? = null,      // Default for optional fields
    val customAliases: List<String> = emptyList(), // Default collections
    val createdAt: Instant,
    val updatedAt: Instant,
) : DTO // Implement marker interface for application DTOs

// ✅ Contract DTO with public visibility
public data class ScopeResult(
    public val id: String,
    public val title: String, 
    public val canonicalAlias: String,
    public val createdAt: Instant,
    public val updatedAt: Instant,
)
```

### 3. Primitive Types Only

DTOs should contain only primitive types to maintain layer separation:

```kotlin
// ✅ Good - primitive types only
data class ScopeDto(
    val id: String,                              // Not ScopeId
    val title: String,                           // Not ScopeTitle  
    val parentId: String?,                       // Not ScopeId?
    val createdAt: Instant,                      // Instant is acceptable
    val aspects: Map<String, List<String>>       // Collections of primitives
)

// ❌ Bad - domain types leak
data class BadScopeDto(
    val id: ScopeId,                             // Domain value object
    val title: ScopeTitle,                       // Domain value object
    val parent: Scope?                           // Domain entity
)
```

## Mapping Between Layers

### Domain to Application Mapping

```kotlin
object ScopeMapper {
    fun toScopeDto(scope: Scope): ScopeDto = ScopeDto(
        id = scope.id.value,                     // Extract primitive from value object
        title = scope.title.value,               // Extract primitive from value object  
        description = scope.description?.value,  // Handle optional value objects
        parentId = scope.parentId?.value,        // Extract primitive from optional ID
        canonicalAlias = scope.canonicalAlias?.value,
        customAliases = scope.customAliases.map { it.value },
        createdAt = scope.createdAt,
        updatedAt = scope.updatedAt,
        aspects = scope.aspects.mapValues { (_, values) -> 
            values.map { it.toString() }        // Convert complex types to strings
        }
    )
}
```

### Application to Contract Mapping

```kotlin
object ScopeContractMapper {
    fun toScopeResult(scopeDto: ScopeDto): ScopeResult = ScopeResult(
        id = scopeDto.id,
        title = scopeDto.title,
        description = scopeDto.description,
        parentId = scopeDto.parentId,
        canonicalAlias = scopeDto.canonicalAlias ?: "", // Handle optionals
        createdAt = scopeDto.createdAt,
        updatedAt = scopeDto.updatedAt,
        isArchived = false, // Set contract-specific defaults
        aspects = scopeDto.aspects
    )
    
    fun fromCreateScopeCommand(command: CreateScopeCommand): CreateScopeInput =
        CreateScopeInput(
            title = command.title,
            description = command.description,
            parentId = command.parentId,
            generateAlias = command.generateAlias,
            customAlias = command.customAlias
        )
}
```

### Complete Data Flow Example

```kotlin
// Step 1: Domain Entity (with rich domain types)
val scope = Scope(
    id = ScopeId.generate(),
    title = ScopeTitle.from("Project Alpha"),
    description = ScopeDescription.from("Strategic project"),
    parentId = ScopeId.from("parent-123"),
    createdAt = Clock.System.now(),
    metadata = mutableMapOf("priority" to "high")
)

// Step 2: Mapper transforms domain entity to DTO
val dto = ScopeMapper.toCreateScopeResult(scope)
// Result: CreateScopeResult(
//   id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
//   title = "Project Alpha", 
//   description = "Strategic project",
//   parentId = "parent-123",
//   createdAt = 2024-01-15T10:30:00Z,
//   metadata = mapOf("priority" to "high")
// )

// Step 3: Handler returns DTO wrapped in Either
return dto.right()
```

## Validation and Documentation

### DTO Documentation

```kotlin
/**
 * Data Transfer Object for Scope entity.
 * Contains only primitive types to maintain layer separation.
 *
 * Used internally within the application layer for:
 * - Query result mapping from domain entities
 * - Input validation before domain entity creation
 * - Response formatting for use case handlers
 *
 * Includes both canonical and custom aliases to provide complete scope information
 * without exposing internal ULID implementation details.
 */
data class ScopeDto(
    /** Unique identifier as string representation of ScopeId ULID */
    val id: String,
    /** Human-readable scope title (validated in domain layer) */
    val title: String,
    /** Optional scope description */
    val description: String?,
    /** Parent scope ID if this is a child scope */
    val parentId: String?,
    /** System-generated canonical alias for this scope */
    val canonicalAlias: String? = null,
    /** User-defined custom aliases for this scope */
    val customAliases: List<String> = emptyList(),
    /** Timestamp when scope was created */
    val createdAt: Instant,
    /** Timestamp when scope was last updated */
    val updatedAt: Instant,
    /** Aspect values as string map for serialization compatibility */
    val aspects: Map<String, List<String>> = emptyMap(),
)
```

## Architecture Testing

Konsist rules to enforce DTO conventions:

```kotlin
"application DTOs should use Dto suffix" {
    Konsist
        .scopeFromDirectory("contexts/*/application")
        .classes()
        .withPackage("..dto..")
        .assert { 
            it.name.endsWith("Dto") || 
            it.name.endsWith("Result") || 
            it.name.endsWith("Input") ||
            it.name == "DTO"
        }
}

"contract DTOs should use appropriate suffixes" {
    Konsist
        .scopeFromDirectory("contracts")
        .classes()
        .assert {
            when {
                it.resideInPackage("..commands..") -> it.name.endsWith("Command")
                it.resideInPackage("..queries..") -> it.name.endsWith("Query")  
                it.resideInPackage("..results..") -> it.name.endsWith("Result") || it.name.endsWith("Info")
                else -> true
            }
        }
}

"DTOs should only contain primitive types" {
    Konsist
        .scopeFromDirectory("contexts/*/application", "contracts")
        .classes()
        .withNameEndingWith("Dto", "Result", "Command", "Query")
        .properties()
        .assert { property ->
            val allowedTypes = setOf(
                "String", "Int", "Long", "Boolean", "Double", "Float",
                "Instant", "LocalDate", "LocalDateTime", "LocalTime",
                "List", "Set", "Map", "Array"
            )
            property.type.sourceType in allowedTypes || 
            property.type.isGeneric || // Allow generic collections
            property.hasNullableType // Allow nullable primitives
        }
}
```

## Migration Strategy

When updating DTO naming conventions:

1. **Add new DTOs** with proper naming alongside existing ones
2. **Update mappers** to support both old and new formats
3. **Gradually migrate usage** from old to new DTOs
4. **Deprecate old DTOs** with clear migration guidance
5. **Remove deprecated DTOs** after grace period

```kotlin
// Phase 1: Add new DTO alongside existing
@Deprecated("Use ScopeResult instead", ReplaceWith("ScopeResult"))
data class ScopeDto(/* existing fields */)

data class ScopeResult(/* new contract-appropriate fields */)

// Phase 2: Update mappers to support both
object ScopeMapper {
    @Deprecated("Use toScopeResult instead")
    fun toScopeDto(scope: Scope): ScopeDto = /* existing mapping */
    
    fun toScopeResult(scope: Scope): ScopeResult = /* new mapping */
}
```

## Key Benefits

- **Clear separation** between application and contract DTOs
- **Consistent naming** conventions across the codebase  
- **Maintainable structure** with domain-based organization
- **Type safety** with primitive-only constraints
- **Documentation** for proper usage and context
- **Architecture enforcement** through automated testing

## Related Documentation

- [Clean Architecture Patterns](./clean-architecture-patterns.md) - Layer responsibilities
- [Error Handling](./error-handling.md) - Error DTO patterns
- [Contracts Slim Policy](./contracts-slim-policy.md) - Contract layer design