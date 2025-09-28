# Cross-Platform E2E Tests

This module contains end-to-end tests for the Scopes CLI and daemon using pre-built native binaries.

## Overview

The E2E tests verify that the native binaries work correctly across different platforms (Linux, macOS, Windows) and architectures (x64, arm64). They test:

### Success Scenarios
- **Scope lifecycle**: Complete CRUD operations, hierarchical management, batch operations
- **Alias management**: Add/remove/resolve aliases, prefix matching, canonical alias changes
- **Aspect management**: Set/update/remove aspects, complex queries, filtering operations
- **Context views**: Create/switch/delete contexts, filtered listings, combined queries
- **Advanced queries**: Comparison operators, OR/NOT logic, nested queries, performance at scale

### Failure Scenarios
- **Daemon failures**: Connection issues, timeouts, port conflicts
- **CLI errors**: Invalid commands, missing arguments, binary issues
- **Environment issues**: Missing directories, permission problems

### Platform-Specific Behaviors
- **File paths**: XDG on Linux, Application Support on macOS, AppData on Windows
- **Permissions**: Unix file permissions vs Windows ACLs
- **Line endings**: LF vs CRLF handling

## Running Tests Locally

### Prerequisites

1. Build the native binaries first:
```bash
./gradlew :apps-scopes:nativeCompile :apps-scopesd:nativeCompile
```

2. Run the E2E tests:
```bash
./gradlew :quality:e2e-tests:e2eTest
```

### Using Pre-built Binaries

You can also test with pre-built binaries:

```bash
# Download binaries (example for Linux x64)
wget https://github.com/kamiazya/scopes/releases/latest/download/scopes-linux-x64
wget https://github.com/kamiazya/scopes/releases/latest/download/scopesd-linux-x64
chmod +x scopes-linux-x64 scopesd-linux-x64

# Run tests with specific binaries
./gradlew :quality:e2e-tests:e2eTest \
  -Pe2e.cli.binary=./scopes-linux-x64 \
  -Pe2e.daemon.binary=./scopesd-linux-x64
```

### Configuration Options

The following system properties can be set:

- `scopes.e2e.cli.binary` - Path to CLI binary
- `scopes.e2e.daemon.binary` - Path to daemon binary
- `scopes.e2e.binary.dir` - Directory containing both binaries
- `scopes.e2e.test.platform` - Platform identifier (linux/darwin/win32)
- `scopes.e2e.test.arch` - Architecture (x64/arm64)
- `scopes.e2e.daemon.startup.timeout` - Daemon startup timeout in ms (default: 30000)
- `scopes.e2e.command.timeout` - Command execution timeout in ms (default: 60000)

## Test Structure

```
e2e-tests/
├── framework/
│   ├── BinaryManager.kt      # Locates and validates binaries
│   ├── PlatformUtils.kt      # Platform-specific utilities
│   ├── DaemonController.kt   # Connects to daemon (no lifecycle mgmt)
│   ├── CliRunner.kt          # Executes CLI commands
│   └── E2ETestBase.kt        # Base test class with setup/teardown
└── tests/
    ├── ScopeLifecycleTest.kt     # Complete scope CRUD operations
    ├── AliasManagementTest.kt    # Alias operations and resolution
    ├── AspectManagementTest.kt   # Aspect CRUD and queries
    ├── ContextViewTest.kt        # Context view management
    ├── AdvancedQueryTest.kt      # Complex queries and performance
    ├── CliIntegrationTest.kt     # Basic CLI command tests
    ├── PlatformSpecificTest.kt   # Platform-specific behaviors
    └── FailureScenarioTest.kt    # Error handling tests
```

Note: Following the philosophy that daemon management is delegated to the OS/package manager, the `DaemonController` only handles connections to existing daemons, not lifecycle management.

## CI Integration

The E2E tests run automatically in GitHub Actions for each platform/architecture combination. See `.github/workflows/e2e-tests.yml` for the workflow configuration.

### Test Matrix

| Platform | Architecture | Status |
|----------|--------------|--------|
| Linux    | x64          | ✅ Required |
| Linux    | arm64        | ⚠️ Experimental |
| macOS    | x64          | ✅ Required |
| macOS    | arm64        | ✅ Required |
| Windows  | x64          | ⚠️ Experimental |
| Windows  | arm64        | ⚠️ Experimental |

## Writing New Tests

Extend `E2ETestBase` to create new tests:

```kotlin
class MyNewTest : E2ETestBase() {
    init {
        describe("My feature") {
            it("should work correctly") {
                // Use cliRunner to execute commands
                val result = cliRunner.create("Test Scope")
                result.requireSuccess()
                
                // Connect to existing daemon if needed
                val daemonInfo = daemonController.connect()
                if (daemonInfo != null) {
                    // Test with connected daemon
                    val stub = daemonController.getControlStub()
                    // ... perform daemon operations
                    daemonController.disconnect()
                }
            }
        }
    }
}
```

Note: The `withDaemon` helper is deprecated as daemon lifecycle is managed by the OS.

## Troubleshooting

### Binary Not Found

If tests fail with "Binary not found", ensure:
1. Binaries are built: `./gradlew nativeCompile`
2. Binary paths are correct (check error message for searched paths)
3. Binaries have execute permission

### Daemon Startup Failures

If daemon fails to start:
1. Check daemon logs in test output
2. Verify no other daemon is running
3. Check endpoint file permissions
4. Try increasing startup timeout

### Platform-Specific Issues

- **Linux**: Ensure `XDG_RUNTIME_DIR` is set or fallback paths are accessible
- **macOS**: Grant necessary permissions for network access
- **Windows**: Run as administrator if permission issues occur

## Known Issues

1. **Native daemon networking**: The GraalVM native daemon may have issues binding to network ports. This is being investigated.

2. **Windows experimental**: Windows builds are marked experimental due to potential native compilation issues.

3. **ARM64 emulation**: Linux ARM64 tests use QEMU emulation which may be slower.