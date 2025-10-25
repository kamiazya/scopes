---
"scopes": patch
---

Migrate from GraalVM Native Image to JAR distribution

**Breaking Change**: Distribution format changed from platform-specific native binaries to universal JAR bundle.

### Migration Details

- **Distribution Format**: Changed from 6 platform-specific native binaries (~260MB total) to single universal JAR bundle (~20MB)
- **Runtime Requirement**: Now requires Java 21+ to be installed on the target system
- **Installation Method**: JAR bundle includes wrapper scripts (bash, batch, PowerShell) for platform-agnostic execution
- **SBOM Structure**: Dual SBOM approach - source-level (`sbom/scopes-sbom.json`) and binary-level (`sbom/scopes-binary-sbom.json`)

### Technical Changes

- Removed GraalVM Native Image build configuration and related plugins
- Implemented Shadow JAR plugin for creating fat JARs with all dependencies
- Updated CI/CD workflows to build universal JAR instead of platform-specific binaries
- Removed platform-specific build jobs (linux-x64, darwin-x64, darwin-arm64, win32-x64, etc.)
- Cleaned up GraalVM-specific configuration files (`native-image.properties`, `reflect-config.json`, etc.)

### Documentation Updates

- Renamed `install-jar.ps1` to `install.ps1` for consistency with `install.sh`
- Removed obsolete native binary documentation (`install/README.md`, `install/verify-README.md`, `install/offline/`)
- Updated all security verification guides to reflect JAR bundle structure
- Updated getting started guide with Java 21 requirement
- Created ADR-0017 documenting the migration decision and rationale

### User Impact

**Benefits**:
- Smaller download size (~20MB vs ~260MB for all platforms)
- Faster startup time and lower memory footprint
- No platform-specific build issues
- Easier to debug and profile with standard JVM tools

**Migration Required**:
- Users must have Java 21+ installed (see docs/explanation/setup/java-setup.md)
- Update installation scripts to use new JAR bundle format
- Replace platform-specific binaries with universal JAR + wrapper scripts
