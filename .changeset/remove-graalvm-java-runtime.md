---
"scopes": major
---

Replace GraalVM Native Image compilation with Java runtime distribution

**BREAKING CHANGE**: Scopes now requires Java 21+ to be installed at runtime. The project no longer produces native binaries.

Key changes:
- Removed all GraalVM Native Image configurations and dependencies
- Replaced native compilation with fat JAR distribution using Shadow pattern
- Updated wrapper scripts to execute JAR files with appropriate JVM checks
- Simplified build process and removed native-image maintenance burden
- Added comprehensive Java version checking in wrapper scripts

Migration guide:
1. Ensure Java 21 or higher is installed on target systems
2. Use `./scopes --build-jar` to build executable JAR files
3. Wrapper scripts automatically handle JAR vs Gradle execution

This change unblocks project development that was stalled due to grpc-netty-shaded incompatibilities with Native Image, particularly around Unix Domain Socket support.
