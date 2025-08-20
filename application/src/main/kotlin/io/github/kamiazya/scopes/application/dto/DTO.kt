package io.github.kamiazya.scopes.application.dto

/**
 * Marker interface for Data Transfer Objects (DTOs).
 *
 * DTOs in this application must follow strict purity rules:
 *
 * 1. **Primitive Types Only**: DTOs may only contain:
 *    - Kotlin primitives (String, Int, Boolean, etc.)
 *    - Standard library types (List, Map, Set, etc.)
 *    - Other DTOs
 *    - Serialization-friendly types (kotlinx.datetime.Instant, UUID, etc.)
 *
 * 2. **No Domain Leakage**: DTOs must NOT contain:
 *    - Domain entities (e.g., Scope, User)
 *    - Domain value objects (e.g., ScopeId, ScopeTitle)
 *    - Domain enums or sealed classes
 *    - Any types from the domain package
 *
 * 3. **Immutability**: DTOs should be immutable data classes
 *
 * 4. **Naming Convention**:
 *    - Command inputs: [Action][Entity] (e.g., CreateScope)
 *    - Query inputs: [Action][Entity]Query (e.g., GetScopeQuery)
 *    - Result outputs: [Action][Entity]Result (e.g., CreateScopeResult)
 *
 * These rules ensure proper layer separation and prevent domain concepts
 * from leaking to the presentation layer.
 */
interface DTO
