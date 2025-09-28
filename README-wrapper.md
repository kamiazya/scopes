# Scopes Development Wrapper Scripts

Gradlew-style wrapper scripts for easy debugging and development of Scopes CLI and daemon.

## Quick Start

```bash
# CLI with default settings
./scopes create "My Task"

# Daemon with default settings
./scopesd

# Show wrapper help
./scopes --help-wrapper
./scopesd --help-wrapper
```

## Development Features

### Debug Mode
```bash
# Enable JVM debug mode (port 5005 for CLI, 5006 for daemon)
./scopes --debug create "Debug Task"
./scopesd --debug

# Custom debug port
./scopes --debug-port 9005 create "Custom Debug"
./scopesd --debug-port 9006
```

### Profiling
```bash
# Enable JVM Flight Recorder profiling
./scopes --profile create "Profile Task"
./scopesd --profile

# Output: scopes-profile.jfr or scopesd-profile.jfr
```

### Log Level Control
```bash
# Set log level for detailed debugging
./scopes --log-level DEBUG list
./scopesd --log-level TRACE
```

### Transport Selection
```bash
# Use gRPC transport (requires daemon running)
./scopes --transport grpc create "gRPC Task"

# Default is local transport
./scopes --transport local create "Local Task"
```

### Build Tasks
```bash
# Run build instead of application
./scopes --gradle-task build
./scopesd --gradle-task test
```

## Configuration

Create `.scopes/wrapper.properties` for persistent settings:

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

## IDE Integration

### VS Code
- Use included `.vscode/tasks.json` for predefined debug tasks
- Use `.vscode/launch.json` for remote debugging configuration

### IntelliJ IDEA
1. Start wrapper with `--debug` flag
2. Create "Remote JVM Debug" configuration
3. Set host: `localhost`, port: `5005` (CLI) or `5006` (daemon)
4. Start debugging

## Examples

```bash
# Debug CLI with gRPC transport and verbose logging
./scopes --debug --transport grpc --log-level DEBUG create "Complex Task"

# Profile daemon with custom settings
./scopesd --profile --host 0.0.0.0 --port 50051 --log-level INFO

# Build and test with custom JVM options
./scopes --gradle-task test --jvm-opts "-Xmx2g -XX:+PrintGCDetails"
```

## Platform Support

- **Unix/Linux/macOS**: `./scopes`, `./scopesd`
- **Windows**: `scopes.bat`, `scopesd.bat`

All wrapper scripts provide identical functionality across platforms.

## Troubleshooting

### Gradle Issues
```bash
# Check Gradle daemon status
./gradlew --status

# Clean and rebuild
./scopes --gradle-task clean
./scopes --gradle-task build
```

### Debug Connection Issues
```bash
# Verify debug port is listening
netstat -an | grep 5005

# Check for port conflicts
./scopes --debug-port 9005 create "Alternative Port"
```

### Performance Issues
```bash
# Increase heap size
./scopes --jvm-opts "-Xmx4g" create "Large Task"

# Enable GC logging
./scopesd --jvm-opts "-XX:+PrintGC -XX:+PrintGCDetails"
```
