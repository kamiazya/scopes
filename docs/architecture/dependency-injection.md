# Dependency Injection Architecture

## Composition Root Pattern

The Scopes project follows the Composition Root pattern for dependency injection. All DI configuration is centralized in the `apps` layer, specifically in the `apps/scopes/src/main/kotlin/.../di/` package.

## Key Principles

1. **No DI in Infrastructure**: Infrastructure modules MUST NOT contain any DI framework dependencies or module definitions
2. **No DI in Domain**: Domain modules remain pure and framework-agnostic
3. **No DI in Interfaces**: Interface modules focus on UI/API concerns without DI configuration
4. **Apps Layer as Composition Root**: The apps layer is the ONLY place where DI modules are defined

## Build Dependencies

The `apps/scopes/build.gradle.kts` file correctly includes dependencies on infrastructure modules:

```kotlin
implementation(project(":scope-management-infrastructure"))
implementation(project(":user-preferences-infrastructure"))
implementation(project(":event-store-infrastructure"))
implementation(project(":device-synchronization-infrastructure"))
```

This is **NOT a violation** of Clean Architecture principles. The Composition Root pattern requires that the apps layer has compile-time dependencies on concrete implementations in order to instantiate and wire them together.

## Module Organization

```
apps/scopes/src/main/kotlin/.../di/
├── CliAppModule.kt                    # Root module aggregating all others
├── ContractsModule.kt                 # Contract/Port bindings
├── ObservabilityModule.kt             # Cross-cutting concerns
├── platform/
│   └── DatabaseModule.kt              # Platform-level infrastructure
├── scopemanagement/
│   ├── ScopeManagementModule.kt       # Application services
│   └── ScopeManagementInfrastructureModule.kt  # Infrastructure implementations
├── userpreferences/
│   └── UserPreferencesModule.kt       # User preferences context
├── eventstore/
│   └── EventStoreInfrastructureModule.kt  # Event store context
└── devicesync/
    └── DeviceSyncInfrastructureModule.kt  # Device sync context
```

## Validation

The architecture is validated by Konsist tests in `CleanArchitectureTest.kt`:

- "DI modules should NOT be in infrastructure layer" - Ensures no DI in infrastructure
- "DI modules should be in apps layer" - Ensures DI is properly located
- "apps layer should not directly depend on infrastructure except for DI" - Allows infrastructure dependencies only in DI modules

## Bootstrap Process

1. `Main.kt` creates a `ScopesCliApplication` instance
2. `ScopesCliApplication` starts Koin with the root `cliAppModule`
3. The root module includes all context-specific modules
4. Each module binds interfaces to concrete implementations
5. Application components are resolved from the container

This architecture ensures proper separation of concerns while maintaining a single, well-defined location for all dependency configuration.
