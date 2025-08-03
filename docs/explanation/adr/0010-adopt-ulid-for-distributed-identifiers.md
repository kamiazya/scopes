# ADR-0010: Adopt ULID for Distributed Identifiers

## Status

Accepted

## Context

Scopes currently uses 8-character hexadecimal identifiers (e.g., `a1b2c3d4`) for domain entities. While this approach is simple, it presents significant challenges for our planned local-first architecture and multi-device synchronization capabilities. This ADR proactively addresses these issues before they become technical debt.

### Anticipated Challenges with the Current ID Scheme

1. **Future Database Performance**: When a local database is introduced, the random nature of the current hex IDs **would cause** severe B-tree index fragmentation. This **would lead** to poor write performance, undermining the responsiveness required for a local-first application.
2. **Device Synchronization Scalability**: The 8-character hex format **lacks** the necessary collision resistance for a distributed environment where multiple devices generate IDs offline. This makes it unsuitable for our device-to-device synchronization roadmap.
3. **Lack of Causal Ordering**: The current IDs contain no temporal information. This **would make it impossible** to establish a reliable causal order of events across devices, which is essential for implementing conflict-free synchronization logic.
4. **Poor Developer Experience**: The generic nature of short hex IDs makes them difficult to distinguish and trace in logs, API responses, and debugging sessions.

### Requirements for a New Identifier Scheme

To address these anticipated challenges, we evaluated modern identifier schemes (including UUIDv4, UUIDv7, CUID2, and ULID) against four core requirements:

1. **K-sortable Properties**: The identifier must be sortable by creation time to ensure high-performance writes and prevent B-tree index fragmentation in our local-first database. This is our most critical requirement.
2. **Global Uniqueness without Coordination**: It must support offline generation on multiple devices without requiring a central authority, ensuring collision resistance for device-to-device synchronization.
3. **Synchronization and Causality Support**: It should embed temporal information to help establish a causal order of events, which is foundational for future conflict-free data synchronization.
4. **Developer Ergonomics**: It must be reasonably human-readable and easy to handle in logs, APIs, and debugging environments.

### Timing Rationale

This decision is made before the initial release, eliminating concerns about backward compatibility. We can implement a complete migration without maintaining legacy ID formats, significantly simplifying the implementation.

## Decision

**We adopt ULID (Universally Unique Lexicographically Sortable Identifier) as the standard identifier format for all domain entities (Scope, Scope, Project) in Scopes.**

### Key Properties of ULID

- **128-bit identifier** with 48 bits for a millisecond-precision timestamp and 80 bits for randomness.
- **Lexicographically sortable** by creation time.
- **Base32 encoded**, resulting in a 26-character, URL-safe, and human-readable string.
- **Collision-resistant** with an astronomically low probability of conflicts.
- **Offline-first**, enabling generation with no network dependency.

### Implementation Approach

1. **Complete Migration**: Replace all existing 8-character hex IDs with ULIDs immediately.
2. **Domain Layer**: Update value objects (ScopeId, ScopeId, ProjectId) to use the ULID format.
3. **Database Storage**: Store ULIDs as `BINARY(16)` for optimal performance while maintaining a string representation in the application layer.
4. **ID Generation**: Implement ULID generation in the application layer using established libraries.

## Consequences

### Positive

- **Dramatically Improved Database Performance**: K-sortable ULIDs will minimize index fragmentation, providing sequential insertion patterns that optimize B-tree performance and reduce write amplification when the database is implemented.
- **Future-Proof for Device Synchronization**: ULIDs provide the foundation for Hybrid Logical Clock (HLC) implementation, enabling causally-ordered event synchronization across multiple devices.
- **Enhanced Developer Experience**: The 26-character Base32 encoding is more readable than hex and is URL-safe, improving debugging, logging, and API interactions.
- **Collision-Free Offline Operation**: 80 bits of randomness ensure global uniqueness across all devices and time periods without coordination.
- **Standards Alignment**: ULID is a well-established specification with mature library implementations across multiple languages.

### Negative

- **Increased Storage Size**: ULIDs require 16 bytes (binary) or 26 characters (string) compared to the 8 characters of the current hex IDs.
- **Breaking Change**: The complete replacement of the existing ID format requires updating all references, though this is mitigated by the pre-release timing.
- **Library Dependency**: Requires the integration of a ULID generation library.

### Neutral

- **Learning Curve**: The development team needs to understand the ULID format and its properties, though this is minimal due to the clear specification.
- **Timestamp Information Exposure**: ULIDs contain creation timestamps, which provides useful debugging information but theoretically reveals timing information.

## Alternatives Considered

### UUIDv4 (Random UUID)

**Rejected**: Fails the k-sortability requirement. While providing global uniqueness, UUIDv4's random nature **would cause** severe database index fragmentation, directly harming local-first application performance.

### UUIDv7 (Time-Ordered UUID)

**Rejected**: Fails the developer ergonomics requirement when compared to ULID. Although technically equivalent to ULID in its binary form, its canonical representation is a 36-character hex string with hyphens. ULID's 26-character Base32 encoding provides a superior developer experience.

### CUID2 (Collision-Resistant Unique Identifier 2)

**Rejected**: Fails the k-sortability requirement. While offering excellent security properties, CUID2 intentionally sacrifices sortability for security. This trade-off is inappropriate for our use case, where database performance is the primary driver.

### Maintaining Current 8-character Hex

**Rejected**: Fails multiple requirements. It **would offer** poor database performance, has insufficient collision resistance for distributed environments, and provides no temporal information for synchronization.

## Related Decisions

- Influences: [ADR-0001: Local-First Architecture](./0001-local-first-architecture.md) - ULID supports the offline ID generation required by local-first principles.
- Influences: [ADR-0003: Adopt Industry Standards](./0003-adopt-industry-standards.md) - ULID is an established standard with broad adoption.
- Related to: [ADR-0007: Domain-Driven Design](./0007-domain-driven-design-adoption.md) - ID management follows DDD value object patterns.

## Scope

- **Bounded Context**: Scope Management Context (primary impact)
- **Components**:
  - All domain entities (Scope, Scope, Project)
  - Value objects (ScopeId, ScopeId, ProjectId)
  - Repository implementations
  - Database schemas
  - API responses and CLI output
- **External Systems**: Future external sync will map ULID internal IDs to external system IDs.

## Tags

`architecture`, `domain-design`, `performance`, `distributed-systems`, `local-first`, `identifiers`
