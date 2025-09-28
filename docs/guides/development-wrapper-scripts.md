# Development Wrapper Scripts Guide

This guide shows how to use the Gradle wrapper-style development scripts for debugging and testing Scopes CLI and daemon during development.

## Overview

Scopes provides wrapper scripts that simplify common development tasks:
- `./scopes` - CLI wrapper for Unix/Linux/macOS
- `./scopesd` - Daemon wrapper for Unix/Linux/macOS  
- `scopes.bat` - CLI wrapper for Windows
- `scopesd.bat` - Daemon wrapper for Windows

These scripts provide a gradlew-like experience with built-in debugging, profiling, and configuration management.

## Basic Usage

### Running CLI Commands

```bash
# Basic command execution
./scopes create "My Development Task"
./scopes list
./scopes --help

# Show wrapper-specific help
./scopes --help-wrapper
```

### Running the Daemon

```bash
# Start daemon with default settings
./scopesd

# Start daemon with specific host/port
./scopesd --host 0.0.0.0 --port 50051

# Show daemon wrapper help
./scopesd --help-wrapper
```

## Development Features

### Debug Mode

Enable JVM debug mode for IDE integration:

```bash
# Enable debug mode (default ports: CLI=5005, daemon=5006)
./scopes --debug create "Debug Task"
./scopesd --debug

# Use custom debug ports
./scopes --debug-port 9005 create "Custom Debug"
./scopesd --debug-port 9006
```

After starting with `--debug`, attach your IDE debugger:
- **IntelliJ IDEA**: Create "Remote JVM Debug" configuration with `localhost:5005`
- **VS Code**: Use included debug configurations in `.vscode/launch.json`

### Log Level Control

Set detailed logging for debugging:

```bash
# Enable debug logging
./scopes --log-level DEBUG list
./scopesd --log-level TRACE

# Available levels: TRACE, DEBUG, INFO, WARN, ERROR
./scopes --log-level TRACE create "Verbose Task"
```

### Transport Selection

Switch between local and gRPC transports:

```bash
# Use local transport (default)
./scopes create "Local Task"

# Use gRPC transport (requires running daemon)
./scopes --transport grpc create "gRPC Task"
```

### Profiling

Enable JVM Flight Recorder for performance analysis:

```bash
# Enable profiling
./scopes --profile create "Performance Test"
./scopesd --profile

# Output files: scopes-profile.jfr, scopesd-profile.jfr
```

### Custom JVM Options

Pass additional JVM options:

```bash
# Increase heap size
./scopes --jvm-opts "-Xmx4g" create "Large Task"

# Enable GC logging
./scopesd --jvm-opts "-XX:+PrintGC -XX:+PrintGCDetails"
```

### Build and Test Tasks

Run different Gradle tasks:

```bash
# Run tests instead of application
./scopes --gradle-task test
./scopesd --gradle-task build

# Clean and rebuild
./scopes --gradle-task clean
./scopes --gradle-task build
```

## Configuration

### Persistent Settings

Create `.scopes/wrapper.properties` for default settings:

```properties
# CLI Configuration
scopes.log.level=DEBUG
scopes.transport=grpc
scopes.jvm.opts=-Xmx1g -XX:+UseG1GC

# Daemon Configuration
scopesd.log.level=INFO
scopesd.host=127.0.0.1
scopesd.port=50051
scopesd.jvm.opts=-Xmx2g -XX:+UseG1GC
```

### Configuration Priority

Settings are applied in this order (highest to lowest priority):
1. Command line arguments
2. `.scopes/wrapper.properties` file
3. Built-in defaults

## Common Development Workflows

### Debugging CLI Issues

```bash
# 1. Start with debug mode and verbose logging
./scopes --debug --log-level DEBUG create "Debug Issue"

# 2. Attach IDE debugger to port 5005

# 3. Reproduce the issue with detailed logs
```

### Testing gRPC Communication

```bash
# 1. Start daemon in debug mode
./scopesd --debug --log-level DEBUG

# 2. In another terminal, use gRPC transport
./scopes --transport grpc --log-level DEBUG create "gRPC Test"

# 3. Monitor both CLI and daemon logs
```

### Performance Analysis

```bash
# 1. Enable profiling on both sides
./scopesd --profile &
./scopes --profile --transport grpc create "Performance Test"

# 2. Analyze generated .jfr files with JProfiler or similar tools
```

### Integration Testing

```bash
# 1. Start daemon with specific settings
./scopesd --debug --host 127.0.0.1 --port 55000

# 2. Run CLI tests against specific endpoint
SCOPES_TRANSPORT=grpc ./scopes create "Integration Test"

# 3. Verify functionality across transport layers
```

## IDE Integration

### VS Code Setup

Use the included configurations:

1. **Debug Tasks**: Use `.vscode/tasks.json` for predefined debug tasks
2. **Launch Configs**: Use `.vscode/launch.json` for remote debugging
3. **Integrated Terminal**: Run wrapper scripts directly in VS Code terminal

### IntelliJ IDEA Setup

1. **Remote Debug Configuration**:
   - Host: `localhost`
   - Port: `5005` (CLI) or `5006` (daemon)
   - Module classpath: Select appropriate module

2. **Run Configurations**:
   - Create shell script run configurations for wrapper scripts
   - Set environment variables as needed

## Troubleshooting

### Build Issues

```bash
# Check Gradle daemon status
./gradlew --status

# Clean build if needed
./scopes --gradle-task clean
./scopes --gradle-task build
```

### Debug Connection Issues

```bash
# Verify debug port is available
netstat -an | grep 5005

# Use alternative debug port if needed
./scopes --debug-port 9005 create "Alternative Debug"
```

### Memory Issues

```bash
# Increase heap size for large operations
./scopes --jvm-opts "-Xmx4g" create "Large Import"

# Monitor GC behavior
./scopesd --jvm-opts "-XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
```

### gRPC Connection Issues

```bash
# Check daemon is running and accessible
./scopes info

# Verify endpoint file exists
ls -la ~/Library/Application\ Support/scopes/run/

# Test with local transport first
./scopes --transport local create "Local Test"
```

## Advanced Usage

### Parallel Development

```bash
# Run multiple instances with different ports
./scopesd --debug-port 5006 --port 50051 &
./scopesd --debug-port 5007 --port 50052 &

# Connect CLI to specific instance
SCOPES_DAEMON_ENDPOINT=127.0.0.1:50052 ./scopes create "Instance 2 Test"
```

### Custom Environment

```bash
# Set custom environment for testing
export SCOPES_LOG_LEVEL=TRACE
export SCOPES_TRANSPORT=grpc
./scopes create "Environment Test"
```

### Automated Testing

```bash
#!/bin/bash
# Integration test script

# Start daemon in background
./scopesd --port 55555 > daemon.log 2>&1 &
DAEMON_PID=$!

# Wait for startup
sleep 5

# Run tests
./scopes --transport grpc create "Auto Test 1"
./scopes --transport grpc create "Auto Test 2"

# Cleanup
kill $DAEMON_PID
```

## Performance Tips

### Gradle Optimization

```bash
# Use Gradle daemon for faster builds
export GRADLE_OPTS="-Dorg.gradle.daemon=true"

# Parallel builds
./scopes --gradle-task build --gradle-opts "--parallel"
```

### JVM Tuning

```bash
# Optimize for development
./scopes --jvm-opts "-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Enable JIT compilation logging
./scopesd --jvm-opts "-XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions"
```

## Related Documentation

- [Development Guidelines](./development/) - Overall development practices
- [Architecture Testing Guide](./architecture-testing-guide.md) - Automated testing with Konsist  
- [CLI Quick Reference](../reference/cli-quick-reference.md) - All CLI commands
- [Clean Architecture](../explanation/clean-architecture.md) - System architecture overview
