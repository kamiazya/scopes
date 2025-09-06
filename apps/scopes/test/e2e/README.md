# E2E Tests for Scopes Native Binary

This directory contains end-to-end tests for the Scopes native binary.

## Test Structure

- `run-native-tests.sh` - Bash script for Unix/Linux/macOS testing
- `run-native-tests.ps1` - PowerShell script for Windows testing

## Running Tests

### Using Gradle Tasks

```bash
# Build native binary and run smoke test
./gradlew :apps-scopes:nativeSmokeTest

# Run full E2E test suite (requires native binary)
./gradlew :apps-scopes:nativeE2eTest
```

### Using Shell Scripts Directly

```bash
# First build the native binary
./gradlew :apps-scopes:nativeCompile

# Run tests with shell script
./apps/scopes/test/e2e/run-native-tests.sh apps/scopes/build/native/nativeCompile/scopes
```

## Test Phases

1. **Phase 1: Basic Execution Tests**
   - Binary executes without errors
   - Help and version flags work correctly

2. **Phase 2: Command Structure Tests**
   - Main commands (scope, context, workspace, etc.) respond to help

3. **Phase 3: Subcommand Tests**
   - Subcommands respond correctly to help flags

4. **Phase 4: Output Format Tests**
   - Version output contains expected information
   - Help output contains usage information

5. **Phase 5: Error Handling Tests**
   - Invalid commands fail gracefully
   - Invalid flags are properly rejected

## CI Integration

The E2E tests are automatically run in GitHub Actions:
- Smoke tests run on all platform builds
- Full E2E test suite runs on primary platforms (Linux x64, Windows x64)
- Tests are skipped for experimental builds to avoid blocking

## Troubleshooting

If tests fail:
1. Check that the binary was built successfully
2. Verify the binary has execution permissions
3. Review test output for specific error messages
4. Run individual test phases to isolate issues
