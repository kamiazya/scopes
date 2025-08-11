# Scopes - AI-Native Task Management System

**ALWAYS follow these instructions first and only fallback to additional search and context gathering if the information here is incomplete or found to be in error.**

Scopes is a next-generation local-first task and project management tool built with Kotlin, designed for symbiotic collaboration between developers and AI assistants. This repository uses a multi-module Clean Architecture approach with Gradle build system.

## Quick Start & Essential Setup

### Prerequisites & Environment Setup
- Install Java 17 or 21 (required): `sudo apt-get update && sudo apt-get install -y openjdk-21-jdk`
- Install GraalVM for native compilation (optional but recommended for CLI builds)
- Verify setup: `java -version` should show Java 17+ (project is configured for Java 21 but works with 17+)

### Bootstrap the Repository
**ALWAYS run these commands in sequence when starting with a fresh clone:**

```bash
# 1. Set executable permissions (required on Unix systems)
chmod +x gradlew scripts/*.sh

# 2. Verify Gradle setup
./gradlew checkGraalVM

# 3. Check current project status
./gradlew tasks --all
```

## ⚠️ Current Known Issues

### JitPack Dependency Resolution
**As of this writing, there is a known network connectivity issue:**
- The `kulid` library dependency (com.github.guepardoapps:kulid:2.0.0.0) cannot be resolved from JitPack.io
- This prevents full compilation and testing until resolved
- **Working commands:** `ktlintFormat`, `ktlintCheck`, `detekt`, `tasks`
- **Failing commands:** `build`, `test`, `run`, `nativeCompile` (all require dependency resolution)

**Workaround:** Focus on code quality and formatting tasks until network connectivity is restored.

## Build & Development Commands

### Core Build Commands
```bash
# Build the project (NEVER CANCEL - takes 3-8 minutes depending on dependencies)
./gradlew build --no-daemon
# Timeout: Set 10+ minutes. Build includes compilation, testing, and quality checks.

# Build without native compilation (faster alternative - takes 2-4 minutes)
./gradlew build --exclude-task nativeCompile --no-daemon
# Timeout: Set 6+ minutes.

# Clean build (NEVER CANCEL - takes 4-10 minutes)
./gradlew clean build --no-daemon
# Timeout: Set 15+ minutes.
```

### Native Binary Compilation
```bash
# Create native binary (NEVER CANCEL - takes 15-45 minutes)
./gradlew nativeCompile --no-daemon
# Timeout: Set 60+ minutes. This builds a standalone executable.

# The native binary will be created at:
# presentation-cli/build/native/nativeCompile/scopes
```

### Testing Commands
```bash
# Run all tests (NEVER CANCEL - takes 2-5 minutes)
./gradlew test --no-daemon
# Timeout: Set 8+ minutes.

# Run tests for specific module
./gradlew :domain:test
./gradlew :application:test
./gradlew :infrastructure:test
./gradlew :presentation-cli:test
```

### Code Quality & Formatting
```bash
# ALWAYS run before committing - Format Kotlin code
./gradlew ktlintFormat --no-daemon

# Check code formatting
./gradlew ktlintCheck --no-daemon

# Run static analysis (takes 1-2 minutes)
./gradlew detekt --no-daemon
# Timeout: Set 4+ minutes.

# Auto-fix all formatting issues (recommended)
./scripts/format-all.sh
```

## Running the Application

### CLI Application
```bash
# After successful build, run the CLI:
./gradlew run --args="--help"

# Or run the native binary (after nativeCompile):
./presentation-cli/build/native/nativeCompile/scopes --help
```

## Validation & Testing Scenarios

**ALWAYS run these validation steps after making changes:**

### 1. Build Validation
```bash
# Verify the project builds successfully
./gradlew build --exclude-task nativeCompile --no-daemon
```

### 2. Code Quality Validation
```bash
# Ensure code meets quality standards
./gradlew ktlintCheck detekt --no-daemon
```

### 3. Functional Testing
```bash
# Test CLI basic functionality (after build)
./gradlew run --args="--help"
# Should display help text without errors

# Test CLI version command
./gradlew run --args="--version"
# Should display version information
```

### 4. Pre-commit Validation
```bash
# Run all pre-commit checks (note: some scripts may have platform-specific issues)
./gradlew ktlintFormat ktlintCheck detekt --no-daemon

# Alternative manual validation:
./gradlew ktlintCheck detekt test --no-daemon
```

## Troubleshooting & Common Issues

### Dependency Resolution Issues
If you encounter "jitpack.io: Temporary failure in name resolution":
- This indicates network connectivity issues with JitPack.io
- **DO NOT** modify dependencies to work around this
- Wait for network connectivity to be restored
- The issue affects the `kulid` library dependency in the domain module

### GraalVM Native Compilation Issues
If `nativeCompile` fails:
- Verify GraalVM is properly installed: `./gradlew checkGraalVM`
- Check Java version is 21: `java -version`
- Ensure you're not running on an unsupported platform

### Build Performance
- Use `--no-daemon` flag for CI environments
- Use `--daemon` flag for local development (faster subsequent builds)
- Gradle daemon improves build performance but uses more memory

## Project Structure & Key Locations

### Module Structure
```
├── domain/              # Core business logic and entities
├── application/         # Use cases and application services  
├── infrastructure/      # External adapters and implementations
├── presentation-cli/    # CLI interface using Clikt
├── docs/               # Documentation following Diátaxis framework
├── scripts/            # Utility scripts for development
└── .github/            # CI/CD workflows and automation
```

### Important Files
- `build.gradle.kts` - Root build configuration
- `gradle/libs.versions.toml` - Dependency version catalog
- `settings.gradle.kts` - Multi-module project settings
- `lefthook.yml` - Git hooks configuration
- `detekt.yml` - Static analysis configuration
- `CLAUDE.md` - Project overview and AI collaboration guidelines

### Configuration Files
- `.editorconfig` - Code formatting rules
- `gradle.properties` - Gradle build configuration
- `lefthook.yml` - Pre-commit hooks

## CI/CD & Automation

### GitHub Workflows
The project uses comprehensive CI/CD with these workflows:
- `build.yml` - Cross-platform native builds (Windows, macOS, Linux)
- `test.yml` - Unit testing across environments
- `code-quality.yml` - Linting and static analysis
- `security.yml` - Security scanning and vulnerability checks

### Pre-commit Hooks (Lefthook)
```bash
# Install pre-commit hooks (recommended)
lefthook install

# Skip hooks temporarily (when needed)
git commit --no-verify
```

## Development Workflow

### Making Changes
1. **ALWAYS** start with `./gradlew ktlintFormat`
2. Make your code changes
3. Run `./gradlew ktlintCheck detekt` before committing
4. Run relevant tests: `./gradlew :module:test`
5. Validate full build: `./gradlew build --exclude-task nativeCompile`

### Before Committing
```bash
# Essential pre-commit checklist:
./gradlew ktlintFormat --no-daemon       # Auto-fix formatting  
./gradlew ktlintCheck detekt --no-daemon # Verify quality
./gradlew test --no-daemon               # Run tests (when dependencies available)
```

### Release Builds
Native compilation for releases:
```bash
# NEVER CANCEL - Full release build takes 45-60 minutes
./gradlew clean build nativeCompile --no-daemon
# Timeout: Set 90+ minutes for full release pipeline
```

## Key Dependencies & Technologies

- **Kotlin 2.2.0** - Primary language
- **Gradle 8.13** - Build system
- **GraalVM** - Native compilation
- **Arrow** - Functional programming
- **Koin** - Dependency injection
- **Clikt** - CLI framework
- **Kotest** - Testing framework
- **Kotlinx Coroutines** - Asynchronous programming

## Performance Expectations

**CRITICAL - NEVER CANCEL these operations:**

| Operation | Expected Time | Timeout Setting |
|-----------|---------------|-----------------|
| `./gradlew build` | 3-8 minutes | 15+ minutes |
| `./gradlew test` | 2-5 minutes | 10+ minutes |
| `./gradlew nativeCompile` | 15-45 minutes | 60+ minutes |
| `./gradlew clean build` | 4-10 minutes | 20+ minutes |
| `./gradlew detekt` | 5-15 seconds | 2+ minutes |
| `./gradlew ktlintFormat` | 10-30 seconds | 2+ minutes |

**Build times vary significantly based on:**
- Network connectivity for dependency resolution
- System performance and available memory
- Whether Gradle daemon is running (faster subsequent builds)
- Clean vs incremental builds

## Emergency Procedures

### If Build Completely Fails
1. Check network connectivity to Maven Central and JitPack
2. Try offline mode if dependencies were previously cached: `./gradlew build --offline`
3. Clean and retry: `./gradlew clean build --no-daemon`
4. Check Java version: `java -version` (must be 21)

### If Tests Fail
1. Run tests in isolation: `./gradlew :module:test` 
2. Check for code style issues: `./gradlew ktlintCheck`
3. Verify no compilation errors: `./gradlew compileKotlin`

### If Native Compilation Fails
1. Verify GraalVM: `./gradlew checkGraalVM`
2. Try building without native compilation: `./gradlew build --exclude-task nativeCompile`
3. Check available disk space (native compilation requires significant space)

**Remember: This is a greenfield project focused on AI-developer collaboration. Always refer to CLAUDE.md for project context and vision.**
