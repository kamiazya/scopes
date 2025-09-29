# GraalVM Native Image (Deprecated)

> ⚠️ **Note**: As of [commit fc70ab3c](https://github.com/kamiazya/scopes/commit/fc70ab3c), Scopes no longer uses GraalVM Native Image for distribution. The project now requires Java 21+ to be installed at the deployment target and distributes as JAR files.

## Background

Previously, Scopes used GraalVM Native Image to create standalone native binaries that didn't require a JVM at runtime. This approach was abandoned due to:

1. **Technical Blockers**: The `grpc-netty-shaded` library had issues with platform-specific transport mechanisms in Native Image mode
2. **Unix Domain Socket Limitations**: Native Image had problems with the UDS implementation required for local IPC
3. **Maintenance Burden**: Keeping Native Image configurations up-to-date was complex and time-consuming
4. **Project Progress**: These issues were blocking overall project advancement

## Current Approach

Scopes now distributes as executable JAR files that require Java 21+ to be installed. This provides:

- **Simplified Distribution**: Single JAR file with all dependencies
- **Better Compatibility**: Full JVM features without Native Image limitations
- **Faster Development**: No need to maintain Native Image configurations
- **Standard Tooling**: Regular Java debugging and profiling tools work

## Migration

If you previously built native images:

1. Remove any GraalVM-specific environment variables
2. Ensure you have Java 21+ installed (any distribution)
3. Build JAR files instead: `./gradlew :apps-scopes:fatJar :apps-scopesd:fatJar`
4. Use the wrapper scripts which now execute the JARs

## Historical Reference

This document is preserved for historical reference. For current installation instructions, see:
- [README.md](../../../README.md#prerequisites)
- [Installation Guide](../../tutorials/getting-started.md#installation)
