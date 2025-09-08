# GraalVM 24 Optimizations

## Overview

This document explains the GraalVM 24 optimizations applied to the Scopes native image build configuration.

## Optimization Features

### 1. SkipFlow Static Analysis (Experimental)

SkipFlow is an experimental extension to Native Image static analysis that tracks primitive values and evaluates branching conditions dynamically during the analysis process.

**Configuration:**
```
-H:+TrackPrimitiveValues
-H:+UsePredicates
```

**Benefits:**
- Up to 4% reduction in binary size
- No additional impact on build time
- Better dead code elimination through branch prediction

### 2. CPU Architecture Optimization

**Configuration:**
```
-march=compatibility
```

**Options:**
- `compatibility`: Ensures binary runs on all x86-64 processors (default, portable)
- `native`: Optimizes for the build machine's specific CPU (faster but less portable)
- Custom targets: e.g., `x86-64-v3` for modern processors with AVX support

**Recommendation:** 
We use `compatibility` for release builds to ensure maximum portability. For local development or specific deployment targets, consider using `-march=native` for better performance.

### 3. Implicit Performance Improvements

GraalVM 24 includes several automatic optimizations:

- **Graal Neural Network (GNN) Static Profiler**: Machine learning-powered profile inference providing up to 7.9% peak performance improvement
- **Enhanced JVMCI Threading**: Default thread count now matches C2 compiler for better warmup performance
- **Improved Vector API Support**: Better optimization for SIMD operations

## Testing Optimizations

To verify the optimizations are working:

1. **Check binary size reduction:**
```bash
# Before optimization
ls -lh build/native/nativeCompile/scopes

# After optimization (should be smaller)
./gradlew clean nativeCompile
ls -lh build/native/nativeCompile/scopes
```

2. **Verify build logs:**
```bash
./gradlew nativeCompile --info | grep -E "(SkipFlow|TrackPrimitiveValues|UsePredicates)"
```

## Compatibility Notes

- These optimizations require GraalVM 24 or later
- SkipFlow is experimental and may change in future releases
- The optimizations are compatible with both Oracle GraalVM and GraalVM Community Edition

## References

- [GraalVM 24 Release Notes](https://www.graalvm.org/release-notes/JDK_24/)
- [Native Image Build Configuration](https://www.graalvm.org/latest/reference-manual/native-image/overview/BuildConfiguration/)
