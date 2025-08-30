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

### Build Configuration

The project uses build-time arguments to configure native image generation. The configuration is defined in `apps/scopes/build.gradle.kts`:

```kotlin
graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopes")
            mainClass.set("io.github.kamiazya.scopes.apps.cli.MainKt")
            useFatJar.set(true)
            
            buildArgs.addAll(listOf(
                "-O2",  // Optimization level
                "--no-fallback",  // Ensure native-only execution
                "--gc=serial",  // Use Serial GC for smaller footprint
                "--report-unsupported-elements-at-runtime",
                "--initialize-at-build-time=kotlin",
                "--initialize-at-build-time=kotlinx.coroutines",
                "--initialize-at-run-time=kotlin.uuid.SecureRandomHolder"
            ))
        }
    }
}
```

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
