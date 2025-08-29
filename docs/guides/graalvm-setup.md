# GraalVM Setup Guide

This guide explains how to set up GraalVM for native image compilation.

## Requirements

- GraalVM JDK 21 or later
- Native Image component
- GraalVM SDK (for build-time configuration)

## Installation Options

### Option 1: Using SDKMAN (Recommended)

```bash
# Install GraalVM JDK 21
sdk install java 21.0.6-graal

# Use GraalVM as current Java version
sdk use java 21.0.6-graal

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

4. Install Native Image component (if not included):

```bash
gu install native-image
```


### Option 3: Using Homebrew (macOS)

```bash
# Install GraalVM
brew install --cask graalvm/tap/graalvm-jdk21

# Set JAVA_HOME (add to your shell profile)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21/Contents/Home
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

## Build Dependencies

### GraalVM SDK Dependency

The project requires the GraalVM SDK as a compile-only dependency for native image builds. This dependency is configured in `apps/scopes/build.gradle.kts`:

```kotlin
dependencies {
    // GraalVM native image
    compileOnly(libs.graalvm.sdk)
}
```

#### Why is GraalVM SDK needed?

The GraalVM SDK is required to compile GraalVM Feature classes that configure native image generation. Specifically:

1. **SQLite JDBC Native Image Support**: The project includes `SqliteJdbcFeature.java` which implements `org.graalvm.nativeimage.hosted.Feature` to properly configure SQLite JDBC driver for native image compilation.

2. **Build-time Configuration**: GraalVM Feature classes allow customization of the native image build process, including:
   - Registering classes for reflection
   - Configuring resource inclusion
   - Setting up JNI access
   - Optimizing initialization timing

3. **GraphQL and Database Support**: Native image compilation requires explicit configuration for dynamic features used by GraphQL schemas and database drivers. The GraalVM SDK enables writing these configurations programmatically rather than through JSON configuration files.

The SDK is marked as `compileOnly` because:
- It's only needed during compilation, not at runtime
- The GraalVM native image builder provides these classes during the native image generation process
- It keeps the JAR size smaller for non-native deployments

## Troubleshooting

### Error: native-image not found

- Ensure JAVA_HOME points to GraalVM installation
- Install Native Image component: `gu install native-image`

### Memory Issues During Compilation

- Increase heap size in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### Optimizing Binary Size

The project is configured with size optimization flags:

- `-Os`: Enables size optimization over performance
- `--gc=serial`: Uses Serial GC with smaller memory footprint
- `-H:+StripDebugInfo`: Removes debug information from binary
- `-H:+ReduceImplicitExceptionStackTraceInformation`: Reduces exception metadata

These flags significantly reduce the native binary size while maintaining functionality.

## CI/CD

GitHub Actions automatically uses GraalVM for native image compilation. Local GraalVM installation is optional for development.
