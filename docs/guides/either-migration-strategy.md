# Either Type Migration Strategy

## Overview
This document outlines the strategy for migrating custom Result types to Arrow's Either pattern across the Scopes codebase.

## Current State Analysis

### Result Type Categories

1. **Sealed Interface Results** (Command/Query responses)
   - AspectResults: 6 types (Create, Update, Delete, Get, List, Validate)
   - ContextViewResults: 6 types (Create, Update, Delete, Get, List, GetActive, SetActive)
   - PreferenceResult: 1 type with variants
   - These represent operation outcomes with success/failure variants

2. **Data Class Results** (DTOs)
   - ScopeResult, CreateScopeResult, UpdateScopeResult, ScopeListResult, AliasListResult
   - SynchronizationResult, ConflictResult, RegisterDeviceResult
   - EventResult
   - These are pure data transfer objects

## Migration Strategy

### Phase 1: Contract Layer Migration
1. Convert sealed interface Results to Either<DomainError, SuccessData>
2. Keep data class Results as-is (they become the Right value)
3. Update port interfaces to return Either types

### Phase 2: Application Layer Updates
1. Update handlers to return Either types
2. Map domain errors to appropriate Left values
3. Ensure consistent error handling patterns

### Phase 3: Infrastructure Layer Updates
1. Update adapters to handle Either types
2. Convert between Either and external representations

### Phase 4: Test Updates
1. Enable ResultTypeEnforcementTest rules
2. Update all tests to work with Either types

## Implementation Patterns

### For Sealed Interface Results
```kotlin
// Before
sealed interface CreateAspectDefinitionResult {
    data class Success(val aspectDefinition: AspectDefinition) : CreateAspectDefinitionResult
    data class AlreadyExists(val key: String) : CreateAspectDefinitionResult
    data class InvalidType(val type: String, val supportedTypes: List<String>) : CreateAspectDefinitionResult
    data class ValidationError(val field: String, val validationFailure: ValidationFailure) : CreateAspectDefinitionResult
}

// After
typealias CreateAspectDefinitionResult = Either<AspectError, AspectDefinition>
// Where AspectError is a sealed class in the domain layer
```

### For Data Class Results
```kotlin
// These remain unchanged as they represent the successful result data
data class ScopeResult(
    val id: String,
    val title: String,
    // ...
)
```

## Benefits
1. Consistent error handling across all layers
2. Composable operations using Either's functional APIs
3. Reduced boilerplate code
4. Better alignment with functional programming principles
5. Type-safe error propagation
