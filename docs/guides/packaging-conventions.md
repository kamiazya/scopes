# Packaging Conventions

This document defines the standardized package structure and naming conventions for the Scopes project, ensuring consistency across all modules and bounded contexts.

## Package Naming Rules

### Base Package Structure

All packages must start with: `io.github.kamiazya.scopes`

### Module Type Prefixes

Each module type has a specific package structure:

```
io.github.kamiazya.scopes.{context}.{layer}
```

Where:
- `{context}` is the bounded context name (e.g., `scopemanagement`, `devicesync`)
- `{layer}` is the architectural layer (e.g., `domain`, `application`, `infrastructure`)

### Contracts Module Special Case

Contracts use a different structure to indicate they are shared interfaces:

```
io.github.kamiazya.scopes.contracts.{context}
```

## Layer-Specific Package Organization

### Domain Layer

The domain layer must organize packages by concept type:

```
io.github.kamiazya.scopes.{context}.domain/
├── entity/          # Domain entities
├── valueobject/     # Value objects
├── aggregate/       # Aggregate roots (if different from entities)
├── repository/      # Repository interfaces
├── service/         # Domain services
├── error/           # Domain-specific errors
├── event/           # Domain events
└── specification/   # Domain specifications/rules
```

**Rules:**
- Each concept should have its own package
- No technical concerns in domain packages
- Errors must be in the `error` package

### Application Layer

The application layer organizes by use case pattern:

```
io.github.kamiazya.scopes.{context}.application/
├── usecase/         # Use case organization
│   ├── command/     # Command definitions
│   ├── query/       # Query definitions  
│   └── handler/     # Handler implementations
│       ├── command/ # Command handlers
│       └── query/   # Query handlers
├── dto/             # Data Transfer Objects
│   ├── {concept}/   # DTOs organized by domain concept
│   ├── common/      # Shared DTOs
│   └── mapper/      # DTO mappers
├── port/            # Port interfaces
│   ├── in/          # Input ports (optional)
│   └── out/         # Output ports (optional)
├── service/         # Application services
├── adapter/         # Port adapters
└── error/           # Application errors
```

**Rules:**
- Commands and queries must be in separate packages
- DTOs should be organized by domain concept
- Mappers should be close to DTOs

### Infrastructure Layer

The infrastructure layer groups by technical concern:

```
io.github.kamiazya.scopes.{context}.infrastructure/
├── repository/      # Repository implementations
├── adapter/         # External system adapters
├── persistence/     # Database-specific code
│   ├── entity/      # JPA/database entities
│   ├── mapper/      # Entity mappers
│   └── sqldelight/  # SQLDelight queries
├── messaging/       # Message queue implementations
├── service/         # Infrastructure services
├── configuration/   # Configuration classes
└── error/          # Infrastructure errors
```

**Rules:**
- Group by technical concern, not domain concept
- Keep persistence details isolated
- Adapters implement application ports

### Contracts Layer

Contracts have a specific organization for API stability:

```
io.github.kamiazya.scopes.contracts.{context}/
├── commands/        # Command DTOs
├── queries/         # Query DTOs
├── results/         # Result DTOs
├── errors/          # Error types
├── events/          # Event DTOs
└── types/           # Shared types
```

**Rules:**
- No implementation code
- Only data structures and interfaces
- Consistent naming suffixes (Command, Query, Result, Error)

### Interface Layer

External interfaces (CLI, REST, GraphQL) follow:

```
io.github.kamiazya.scopes.interfaces.{type}/
├── commands/        # Interface commands
├── adapters/        # Port adapters
├── formatters/      # Output formatters
├── validators/      # Input validators
├── mappers/         # Interface-specific mappers
├── core/            # Core interface utilities
└── error/          # Interface errors
```

## Naming Conventions

### Package Names

- **All lowercase**: `scopemanagement` not `scopeManagement`
- **No underscores**: `devicesync` not `device_sync`
- **Singular form**: `entity` not `entities`
- **Domain concepts**: `valueobject` not `vo`

### Special Package Names

- `common`: Shared utilities within a module
- `core`: Essential abstractions
- `internal`: Module-private implementations (use sparingly)
- `test`: Test utilities (in test source sets only)

## Cross-Cutting Concerns

### Platform Packages

Platform modules use a different structure:

```
io.github.kamiazya.scopes.platform.{concern}/
├── commons/         # Shared abstractions
├── domain/          # Domain utilities
├── application/     # Application utilities
└── infrastructure/  # Infrastructure implementations
```

### Shared Kernel

Truly shared domain concepts:

```
io.github.kamiazya.scopes.sharedkernel/
├── identity/        # Shared identity types
├── time/            # Time abstractions
└── event/          # Base event types
```

## Package Dependencies Rules

### Allowed Dependencies

1. **Domain Layer**:
  - May depend on: platform-domain-commons, shared kernel
  - May NOT depend on: application, infrastructure, contracts

2. **Application Layer**:
  - May depend on: domain, contracts, platform-application-commons
  - May NOT depend on: infrastructure, interfaces

3. **Infrastructure Layer**:
  - May depend on: domain, application, platform-infrastructure
  - May NOT depend on: interfaces

4. **Contracts Layer**:
  - May depend on: platform-commons (minimal)
  - May NOT depend on: any other layer

5. **Interface Layer**:
  - May depend on: application, contracts
  - May NOT depend on: domain, infrastructure

### Import Rules

- **No wildcard imports**: Use explicit imports
- **No circular dependencies**: Enforced by build
- **No implementation leakage**: Interfaces only expose contracts

## Migration Guidelines

When refactoring existing packages:

1. **Create new structure alongside old**
2. **Gradually move classes with tests**
3. **Update imports incrementally**
4. **Remove old packages when empty**

## Enforcement

These conventions are enforced by:

1. **Konsist tests**: Automated architecture tests
2. **Build configuration**: Module dependencies
3. **Code review**: Manual verification
4. **IDE templates**: Project structure templates

## Examples

### Good Package Structure

```kotlin
// Domain entity
package io.github.kamiazya.scopes.scopemanagement.domain.entity

// Application DTO  
package io.github.kamiazya.scopes.scopemanagement.application.dto.scope

// Infrastructure adapter
package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapter

// Contract command
package io.github.kamiazya.scopes.contracts.scopemanagement.commands
```

### Bad Package Structure

```kotlin
// Wrong: Mixed case
package io.github.kamiazya.scopes.scopeManagement.domain.entity

// Wrong: Technical grouping in domain
package io.github.kamiazya.scopes.scopemanagement.domain.dto

// Wrong: Plural form
package io.github.kamiazya.scopes.scopemanagement.domain.entities

// Wrong: Implementation in contracts
package io.github.kamiazya.scopes.contracts.scopemanagement.impl
```

## Benefits

Following these conventions provides:

1. **Discoverability**: Easy to find code by type and purpose
2. **Consistency**: Same patterns across all contexts
3. **Maintainability**: Clear boundaries and dependencies
4. **Onboarding**: New developers understand structure quickly
5. **Tool Support**: IDE and build tools work better with conventions
