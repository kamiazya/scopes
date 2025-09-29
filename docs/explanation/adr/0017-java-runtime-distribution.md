# ADR-0017: Adopt Java Runtime Distribution Over Native Compilation

## Status

Accepted

## Context

The Scopes project initially pursued GraalVM Native Image compilation to create standalone native binaries that wouldn't require a JVM at runtime. The primary motivation was to simplify distribution and eliminate the Java runtime dependency for end users.

However, several critical technical challenges emerged:

1. **gRPC Implementation Constraints**: The `grpc-netty-shaded` library, essential for our gRPC communication layer, has fundamental incompatibilities with GraalVM Native Image when using platform-specific transport mechanisms (Unix Domain Sockets and native epoll/kqueue).

2. **Development Velocity Impact**: Maintaining Native Image configurations for reflection, JNI, resources, and serialization became a significant burden. Each library update potentially required configuration adjustments.

3. **Platform-Specific Transport Issues**: Our attempt to implement Unix Domain Socket (UDS) support for local IPC hit insurmountable obstacles with Native Image's handling of platform-specific native code in the Netty transport layer.

4. **Debugging and Tooling Limitations**: Native Image compilation removed access to standard JVM debugging tools, profilers, and diagnostic capabilities that are invaluable during development and production troubleshooting.

5. **Project Progress Blocker**: These technical issues were preventing forward progress on core features, as significant effort was being spent on Native Image compatibility rather than business logic.

## Decision

We will distribute Scopes as executable JAR files requiring Java 21+ runtime instead of native binaries. The distribution model includes:

1. **Fat JAR Distribution**: Create self-contained JAR files with all dependencies using the Shadow plugin pattern
2. **Java 21+ Requirement**: Require users to have Java 21 or higher installed
3. **Wrapper Script Enhancement**: Update wrapper scripts to execute JARs with appropriate JVM options
4. **TCP-Only Transport**: Remove Unix Domain Socket support in favor of TCP sockets for all gRPC communication

## Consequences

### Positive
- **Immediate Unblocking**: Project can proceed with core feature development without Native Image constraints
- **Full JVM Features**: Access to all Java features, libraries, and runtime optimizations
- **Standard Tooling**: Full access to JVM debugging tools, profilers, and monitoring solutions
- **Simplified Maintenance**: No need to maintain Native Image configurations
- **Better Compatibility**: Broader library ecosystem compatibility without Native Image restrictions
- **Faster Development Cycle**: No long Native Image compilation times during development

### Negative
- **Runtime Dependency**: Users must install Java 21+, adding a deployment prerequisite
- **Larger Memory Footprint**: JVM overhead compared to native binaries
- **Slower Startup Time**: JVM startup penalty (though mitigated by modern JVM improvements)
- **Distribution Size**: No longer single binary distribution (though JAR + JVM is still reasonable)

### Neutral
- **Security Model Change**: Shifts from binary-level to JVM-level security considerations
- **Performance Characteristics**: Different performance profile (JVM warmup vs native consistency)
- **Cross-Platform Story**: Still cross-platform but through JVM rather than native compilation

## Alternatives Considered

### Alternative 1: Fix Native Image Compatibility
**Description**: Invest significant effort to resolve all Native Image compatibility issues, potentially switching gRPC implementations or writing custom transport layers.
**Rejection Reason**: The effort required would be disproportionate to the benefit, potentially taking months with uncertain success. The grpc-netty-shaded issues are fundamental architectural mismatches.

### Alternative 2: Different Native Compilation Tool
**Description**: Explore alternatives like Quarkus with Mandrel or other AOT compilation solutions.
**Rejection Reason**: Similar reflection/JNI challenges exist across native compilation tools. Would require significant rearchitecture without guaranteed success.

### Alternative 3: Hybrid Approach
**Description**: Provide both JAR and native distributions, maintaining two distribution channels.
**Rejection Reason**: Doubles maintenance burden and testing requirements. Native Image issues would still block features that depend on affected libraries.

### Alternative 4: Rewrite in Native Language
**Description**: Port the project to Rust, Go, or another language with native compilation.
**Rejection Reason**: Massive effort that would essentially restart the project. Loses the benefits of the JVM ecosystem and Kotlin language features.

## Related Decisions

- Influenced by: [ADR-0001: Local-First Architecture](./0001-local-first-architecture.md) - Local-first still maintained through embedded database
- Influences: Distribution and installation documentation
- Related to: [ADR-0008: Clean Architecture](./0008-clean-architecture-adoption.md) - Architecture remains unchanged

## Scope

- **Bounded Context:** All contexts affected by distribution model
- **Components:** 
  - Build configuration (Gradle)
  - Wrapper scripts (scopes, scopesd)
  - Installation documentation
  - CI/CD pipelines
- **External Systems:** 
  - Package distribution mechanisms
  - User installation procedures

## Tags

`architecture`, `distribution`, `runtime`, `graalvm`, `java`, `decision`
