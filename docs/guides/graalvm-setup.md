# GraalVM Setup Guide

This guide explains how to set up GraalVM for native image compilation.

## Requirements

- GraalVM JDK 21 or later
- Native Image component
- GraalVM SDK (optional, for build-time configuration)

## Installation Options

### Option 1: Using SDKMAN (Recommended)

```bash
# Install GraalVM JDK 21
sdk install java 21.0.6-graal

# Use GraalVM as current Java version
sdk use java 21.0.6-graal

# Install Native Image component (if not included)
gu install native-image

# Verify installation
java -version
native-image --version
```

### Option 2: Manual Installation

1. Download GraalVM from [https://www.graalvm.org/downloads/](https://www.graalvm.org/downloads/)
2. Extract and set JAVA_HOME:

```bash
export JAVA_HOME=/path/to/graalvm
export PATH=$JAVA_HOME/bin:$PATH
```

3. Install Native Image component (if not included):

```bash
gu install native-image
```


### Option 3: Using Homebrew (macOS)

```bash
# Install GraalVM
brew install --cask graalvm/tap/graalvm-jdk21

# Set JAVA_HOME (add to your shell profile)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# Install Native Image component (if not included)
gu install native-image
```

## Verification

After installation, verify that everything works:

```bash
# Check Java version
java -version

# Check native-image availability
native-image --version

# Test native compilation
./gradlew nativeCompile
```

## Native Image Configuration

### Build Configuration Concept

The project is configured for native image generation with the following principles:

- **Size Optimization**: Uses `-Os` flag and other size-reduction options to minimize binary size
- **Performance**: Configured with Serial GC for smaller memory footprint
- **Kotlin Support**: Special initialization settings for Kotlin and Kotlinx Coroutines
- **Development vs Production**: Runtime reports are enabled only for local development, not in CI/release builds

The actual build configuration is managed in the build scripts and CI pipeline.

### GraalVM SDK Dependency (Optional)

The GraalVM SDK is included as a compile-only dependency for compatibility with CI/CD pipelines and future extensibility:

```kotlin
dependencies {
    // GraalVM native image - for potential Feature class implementations
    compileOnly(libs.graalvm.sdk)
}
```

#### Current Status

- **Not actively used**: The project currently uses build arguments for all native image configuration
- **CI/CD compatibility**: Included to prevent build failures in environments that may expect Feature class support
- **Future-ready**: Available if custom Feature classes become necessary

#### When You Might Need Feature Classes

Feature classes (`org.graalvm.nativeimage.hosted.Feature`) would be needed for:

1. **Complex reflection registration**: When build arguments become insufficient
2. **Database driver configuration**: If SQLite or other drivers require custom native image setup
3. **Dynamic proxy generation**: For frameworks that generate classes at runtime
4. **Resource bundle management**: For internationalization support

For now, the simple build argument approach in `graalvmNative` configuration is sufficient for the project's needs.

## Troubleshooting

### Error: native-image not found

- Ensure JAVA_HOME points to GraalVM installation
- Install Native Image component: `gu install native-image`

### Memory Issues During Compilation

Native image compilation requires significant memory. If you encounter out-of-memory errors, increase the JVM heap size in your Gradle configuration.

### Optimizing Binary Size

The project is configured with size optimization flags:

- `-Os`: Enables size optimization over performance
- `--gc=serial`: Uses Serial GC with smaller memory footprint
- `-H:+StripDebugInfo`: Removes debug information from binary
- `-H:+ReduceImplicitExceptionStackTraceInformation`: Reduces exception metadata

These flags significantly reduce the native binary size while maintaining functionality.

## CI/CD

GitHub Actions automatically uses GraalVM for native image compilation. Local GraalVM installation is optional for development.
