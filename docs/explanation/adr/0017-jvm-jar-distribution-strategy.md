# ADR-0017: JVM JAR Distribution Strategy

## Status

Accepted

## Context

The Scopes project has been using GraalVM Native Image for distribution, creating platform-specific native binaries for Linux, macOS, and Windows (6 variants total). While this approach provides fast startup times and reduced memory footprint, several critical issues have emerged:

### Issues with GraalVM Native Image

1. **gRPC Incompatibility**
   - gRPC is tightly coupled with JVM internals
   - Requires extensive reflection configuration and initialization flags
   - Makes daemon-to-CLI communication via gRPC impractical
   - The planned `scopesd` daemon with gRPC-based IPC is blocked by these limitations

2. **Build Complexity**
   - Extensive native-image configuration required (`reflect-config.json`, `jni-config.json`, etc.)
   - Platform-specific build flags and workarounds (e.g., SQLite JNI handling)
   - Fragile initialization timing (`--initialize-at-build-time`, `--initialize-at-run-time`)
   - Configuration cache disabled due to plugin incompatibility
   - Maintenance burden increases with each dependency update

3. **Development Friction**
   - Slow build times (2-5 minutes per platform)
   - Difficult debugging of native image failures
   - Limited library ecosystem support
   - Requires GraalVM toolchain setup for contributors

4. **Current State**
   - `scopesd` daemon is skeleton-only (no IPC implementation)
   - gRPC integration for daemon communication is blocked
   - Native image benefits are theoretical rather than practical at this stage

### Project Characteristics

- **Primary Interface**: CLI-first architecture (ADR-0005)
- **Technology Stack**: Kotlin/JVM with standard libraries
- **Standards-First Approach**: Adopt industry standards over custom solutions (ADR-0003)
- **Distribution Requirements**:
  - Cross-platform support (Linux, macOS, Windows)
  - Easy installation for end users
  - Security verification (SLSA, SBOM)
  - Offline/air-gapped deployment support

### Industry Standard JVM Distribution

Modern JVM applications typically use one of these approaches:

1. **Self-Contained JAR** (Fat/Uber JAR)
   - Single executable JAR with all dependencies
   - Platform-independent
   - Standard Java launcher (`java -jar app.jar`)
   - Industry-standard approach for CLI tools (Gradle, Maven, etc.)

2. **Application Plugins** (jlink, jpackage)
   - Custom JVM runtime bundled with application
   - Platform-specific installers
   - Larger distribution size
   - Better for GUI applications

3. **Container Distribution**
   - Docker/OCI images
   - Ideal for server-side applications
   - Kubernetes-native deployment

## Decision

We will transition from GraalVM Native Image to **Fat JAR distribution** with platform-specific wrapper scripts, following industry-standard JVM application distribution practices.

### Distribution Strategy

#### Primary Distribution: Executable Fat JAR

1. **Fat JAR Creation**
   - Use Gradle Shadow plugin to create self-contained executable JAR
   - Include all dependencies in a single JAR file
   - Set manifest with main class for direct execution
   - JAR can be executed as: `java -jar scopes.jar <commands>`

2. **Platform-Specific Wrapper Scripts**
   - **Unix (`scopes`)**: Bash wrapper script
     ```bash
     #!/bin/bash
     exec java -jar "${BASH_SOURCE%/*}/scopes.jar" "$@"
     ```
   - **Windows (`scopes.bat`)**: Batch wrapper script
     ```batch
     @echo off
     java -jar "%~dp0scopes.jar" %*
     ```
   - **PowerShell (`scopes.ps1`)**: Optional PowerShell wrapper
     ```powershell
     & java -jar "$PSScriptRoot/scopes.jar" $args
     ```

3. **Installation Bundles**
   - Platform-specific bundles similar to current structure
   - Each bundle contains:
     - Fat JAR (`scopes.jar`)
     - Wrapper scripts appropriate for platform
     - JVM detection and installation guidance
     - SHA256 hash verification
     - SLSA provenance
     - SBOM files
     - Installation script

#### JVM Runtime Requirements

- **Minimum Version**: Java 21 (LTS)
- **Detection Strategy**: Installation script checks for Java availability
- **User Guidance**: If Java not found, provide installation instructions
  - Linux: Package manager (apt, yum, pacman)
  - macOS: Homebrew or SDKMAN
  - Windows: Chocolatey, Scoop, or direct download

#### Future Optimization: jlink Custom Runtime (Optional)

For users who want smaller distribution and don't have Java installed:

- Use `jlink` to create minimal JVM runtime
- Bundle custom runtime with application
- Platform-specific distribution (~50-80MB vs ~20MB JAR)
- Implemented as alternative distribution method, not replacement

### Migration Plan

#### Phase 1: Parallel Implementation (Current Release Cycle)

1. Add Shadow plugin to `apps/scopes/build.gradle.kts`
2. Create wrapper scripts for each platform
3. Update installation scripts to handle JAR + scripts
4. Add JVM detection and guidance
5. Test installation on all platforms
6. Update documentation

#### Phase 2: Transition Release (Next Release)

1. Publish both Native Binary and JAR distributions
2. Mark Native Binary as deprecated
3. Collect user feedback
4. Monitor adoption metrics

#### Phase 3: JAR-Only Distribution (Following Release)

1. Remove GraalVM Native Image build configuration
2. Simplify CI/CD pipeline
3. Update all documentation
4. Archive Native Binary build knowledge for reference

### Build Configuration Changes

```kotlin
// apps/scopes/build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)  // Add Shadow plugin
    application
}

application {
    mainClass.set("io.github.kamiazya.scopes.apps.cli.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("scopes")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    manifest {
        attributes["Main-Class"] = "io.github.kamiazya.scopes.apps.cli.MainKt"
        attributes["Multi-Release"] = "true"
    }

    // Merge service files properly
    mergeServiceFiles()
}

// Remove GraalVM plugin and configuration
// plugins {
//     alias(libs.plugins.graalvm.native)
// }
```

### Distribution Package Structure

```text
scopes-1.0.0-dist/
├── scopes.jar                    # Fat JAR (all platforms)
├── bin/
│   ├── scopes                    # Unix wrapper
│   ├── scopes.bat                # Windows batch wrapper
│   └── scopes.ps1                # Windows PowerShell wrapper
├── verification/
│   ├── scopes.jar.sha256         # JAR hash
│   └── multiple.intoto.jsonl     # SLSA provenance
├── sbom/
│   ├── scopes-sbom.json          # CycloneDX JSON
│   └── scopes-sbom.xml           # CycloneDX XML
├── docs/
│   ├── INSTALL.md                # Installation guide
│   ├── SECURITY.md               # Security verification
│   └── JAVA_SETUP.md             # Java installation guide
├── install.sh                    # Unix installer
├── install.ps1                   # Windows installer
└── README.md                     # Quick start
```

### Security Considerations

1. **JAR Verification**
   - SHA256 hash included for JAR file
   - SLSA provenance for build integrity
   - SBOM unchanged (CycloneDX format)

2. **Wrapper Script Security**
   - Scripts use relative paths to locate JAR
   - No remote execution or downloads
   - Clear separation of JAR and wrapper concerns

3. **Java Runtime Security**
   - Users install Java from trusted sources
   - Installation script recommends official distributions
   - Security updates handled by Java runtime updates

### Documentation Updates Required

1. **Installation Documentation**
   - Add Java runtime requirements
   - Update installation procedures
   - Add Java installation guides per platform
   - Update quick start guides

2. **Development Documentation**
   - Remove GraalVM setup requirements
   - Simplify build instructions
   - Update CI/CD documentation

3. **User Documentation**
   - Explain JAR distribution advantages
   - Add troubleshooting for Java issues
   - Update performance expectations

## Consequences

### Positive

1. **Simplified Development**
   - Standard JVM toolchain (no GraalVM required)
   - Faster build times (30s vs 2-5min per platform)
   - Easier debugging (standard JVM tools)
   - No native-image configuration maintenance
   - Contributor onboarding simplified

2. **gRPC Enablement**
   - Full gRPC support for daemon communication
   - Standard JVM networking libraries work without issues
   - Future IPC implementations unblocked

3. **Reduced Complexity**
   - Single JAR instead of 6+ platform binaries
   - No platform-specific build workarounds
   - Simpler CI/CD pipeline
   - Easier dependency updates

4. **Industry Standard Approach**
   - Aligns with ADR-0003 (adopt industry standards)
   - Follows established JVM distribution practices
   - Better ecosystem compatibility
   - More familiar to Java/Kotlin developers

5. **Improved Maintainability**
   - Fewer configuration files
   - Standard Gradle shadow plugin
   - Well-documented patterns
   - Large community support

### Negative

1. **Java Runtime Dependency**
   - Users must have Java 21+ installed
   - Increased initial download size (if using jlink)
   - Additional installation step for users without Java

2. **Startup Performance**
   - Slower startup time (~200-500ms vs ~50ms native)
   - Higher memory baseline (~50-100MB vs ~10-20MB native)
   - Less important for CLI tool with persistent daemon

3. **Distribution Size**
   - Fat JAR: ~20-30MB (single file, all platforms)
   - With jlink runtime: ~50-80MB per platform
   - Current Native: ~20MB per platform × 6 = ~120MB total

4. **Perception**
   - Some users prefer native binaries
   - "Requires Java" may reduce adoption
   - Mitigated by clear documentation and easy installation

### Neutral

1. **Security Model**
   - Same SLSA/SBOM verification
   - Java runtime security is external concern
   - Clear separation of responsibilities

2. **Performance**
   - JIT compilation provides good long-running performance
   - Startup overhead acceptable for CLI tool
   - Daemon model benefits from JVM optimizations

3. **Future Options**
   - Can still provide jlink runtime bundles
   - Native image remains option for specific use cases
   - GraalVM improvements may make it viable again
   - Modular JAR distribution for CLI + Daemon shared libraries (see below)

## Implementation Notes

### Critical Path

1. **Shadow JAR Configuration**
   - Add Shadow plugin dependency
   - Configure JAR manifest
   - Test all commands with JAR execution

2. **Wrapper Scripts**
   - Create and test Unix wrapper
   - Create and test Windows wrappers
   - Ensure proper argument passing

3. **Installation Scripts**
   - Update to install JAR + wrappers
   - Add Java detection logic
   - Provide Java installation guidance

4. **CI/CD Updates**
   - Modify build workflow
   - Update release workflow
   - Simplify artifact generation

5. **Documentation**
   - Update installation guides
   - Add Java setup documentation
   - Update troubleshooting guides

### Testing Requirements

1. **Platform Testing**
   - Linux (Ubuntu, Fedora, Arch)
   - macOS (x64, ARM64)
   - Windows (PowerShell, CMD)

2. **Java Version Testing**
   - Java 21 (minimum)
   - Java 23 (current)
   - Future Java versions

3. **Installation Testing**
   - Fresh installation (no Java)
   - Installation with existing Java
   - Upgrade from native binary

### Rollback Plan

If JAR distribution proves problematic:

1. Keep Native Image build configuration in git history
2. Can revert to Native Image for specific platforms
3. Provide both distributions during transition
4. User feedback determines final approach

## Future Enhancement: Modular JAR Distribution

When the `scopesd` daemon is implemented and both CLI and daemon need to be distributed, we can optimize the distribution using modular JARs to share common libraries.

### Motivation for Modular Distribution

With both CLI (`scopes`) and Daemon (`scopesd`) applications:

- **Current Fat JAR approach**: Each application bundles all dependencies
  - `scopes.jar`: ~20MB (includes platform, contexts, interfaces)
  - `scopesd.jar`: ~20MB (includes platform, contexts, interfaces)
  - **Total**: ~40MB with significant duplication

- **Modular JAR approach**: Share common libraries
  - `scopes-common.jar`: ~15MB (platform, contexts, shared interfaces)
  - `scopes-cli.jar`: ~5MB (CLI-specific code)
  - `scopesd.jar`: ~5MB (Daemon-specific code)
  - **Total**: ~25MB (37.5% reduction)

### Proposed Modular Structure

```text
scopes-1.0.0-dist/
├── lib/
│   ├── scopes-common-1.0.0.jar     # Shared: platform, contexts, contracts
│   ├── scopes-cli-1.0.0.jar        # CLI-specific: interfaces/cli, apps/scopes
│   └── scopesd-1.0.0.jar           # Daemon-specific: interfaces/daemon, apps/scopesd
├── bin/
│   ├── scopes                      # CLI wrapper (uses lib/*)
│   └── scopesd                     # Daemon wrapper (uses lib/*)
├── verification/
│   ├── scopes-common-1.0.0.jar.sha256
│   ├── scopes-cli-1.0.0.jar.sha256
│   └── scopesd-1.0.0.jar.sha256
└── ...
```

### Wrapper Script Example

```bash
#!/bin/bash
# bin/scopes (CLI wrapper)
SCOPES_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLASSPATH="$SCOPES_HOME/lib/scopes-common-1.0.0.jar"
CLASSPATH="$CLASSPATH:$SCOPES_HOME/lib/scopes-cli-1.0.0.jar"

exec java -cp "$CLASSPATH" \
  io.github.kamiazya.scopes.apps.cli.MainKt "$@"

# bin/scopesd (Daemon wrapper)
SCOPES_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLASSPATH="$SCOPES_HOME/lib/scopes-common-1.0.0.jar"
CLASSPATH="$CLASSPATH:$SCOPES_HOME/lib/scopesd-1.0.0.jar"

exec java -cp "$CLASSPATH" \
  io.github.kamiazya.scopes.apps.daemon.MainKt "$@"
```

### Build Configuration Changes

```kotlin
// New module: scopes-common (bundles shared code)
project(":scopes-common") {
    plugins {
        `java-library`
    }

    dependencies {
        // Platform layer
        api(project(":platform:commons"))
        api(project(":platform:application-commons"))
        api(project(":platform:observability"))

        // Bounded contexts
        api(project(":contexts:scope-management:domain"))
        api(project(":contexts:scope-management:application"))
        api(project(":contexts:scope-management:infrastructure"))
        api(project(":contexts:user-preferences:domain"))
        api(project(":contexts:user-preferences:application"))
        api(project(":contexts:user-preferences:infrastructure"))

        // Shared contracts
        api(project(":contracts:scope-management"))
        api(project(":contracts:user-preferences"))

        // Shared interfaces
        api(project(":interfaces:shared"))
    }
}

// apps/scopes: CLI application (depends on common)
project(":apps:scopes") {
    dependencies {
        implementation(project(":scopes-common"))
        implementation(project(":interfaces:cli"))
    }

    tasks.jar {
        // Thin JAR (excludes scopes-common dependencies)
        archiveBaseName.set("scopes-cli")

        manifest {
            attributes["Main-Class"] = "io.github.kamiazya.scopes.apps.cli.MainKt"
            attributes["Class-Path"] = "scopes-common-${project.version}.jar"
        }
    }
}

// apps/scopesd: Daemon application (depends on common)
project(":apps:scopesd") {
    dependencies {
        implementation(project(":scopes-common"))
        implementation(project(":interfaces:daemon"))
    }

    tasks.jar {
        // Thin JAR (excludes scopes-common dependencies)
        archiveBaseName.set("scopesd")

        manifest {
            attributes["Main-Class"] = "io.github.kamiazya.scopes.apps.daemon.MainKt"
            attributes["Class-Path"] = "scopes-common-${project.version}.jar"
        }
    }
}
```

### Benefits of Modular Approach

1. **Reduced Distribution Size**: ~37% smaller than dual Fat JARs
2. **Efficient Updates**: Update common library once, both apps benefit
3. **Clear Separation**: Explicit dependency boundaries
4. **Version Consistency**: Shared code guaranteed to be same version
5. **Standard Java Pattern**: Uses standard classpath mechanism

### Migration Path

1. **Phase 1** (Current): Single Fat JAR for CLI only
2. **Phase 2**: When daemon is implemented, introduce modular structure
3. **Phase 3**: Optionally provide both Fat JAR and Modular distributions

### Compatibility

- Users who want single-file distribution can still use Fat JAR
- Users who want smaller distribution can use modular approach
- Both can be provided simultaneously (different download options)

This approach maintains flexibility while optimizing for the multi-application scenario.

## Related ADRs

- ADR-0003: Adopt Industry Standards - JAR distribution is industry-standard for JVM applications
- ADR-0005: CLI-First Interface Architecture - Maintains CLI as primary interface
- Future: gRPC/IPC implementation for daemon communication (unblocked by this decision)

## References

- [Gradle Shadow Plugin](https://github.com/johnrengelman/shadow)
- [Java Packaging Tools](https://docs.oracle.com/en/java/javase/21/jpackage/)
- [jlink Documentation](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html)
- [GraalVM Native Image Limitations](https://www.graalvm.org/latest/reference-manual/native-image/metadata/#reflection)
- [gRPC Java](https://grpc.io/docs/languages/java/)
