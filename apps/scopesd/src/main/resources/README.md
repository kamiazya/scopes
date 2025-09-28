# Scopes Daemon Service Definition Files

This directory contains service definition files for running the Scopes daemon as an OS-managed service.

## Directory Structure

```
resources/
├── systemd/                    # Linux systemd service definitions
│   ├── scopesd.service         # System-wide service (requires root)
│   └── scopesd.user.service    # User-level service (no root required)
├── launchd/                    # macOS launchd service definitions
│   ├── com.github.kamiazya.scopesd.plist      # System-wide service
│   └── com.github.kamiazya.scopesd.user.plist # User-level service
└── README.md                   # This file
```

## Service Features

### Common Features (All Platforms)
- **Automatic restart on failure** with throttling
- **Resource limits** to prevent excessive resource usage
- **Log rotation** and centralized logging
- **Clean shutdown** with graceful termination
- **Ephemeral port binding** (127.0.0.1:0)
- **Endpoint file creation** for CLI discovery

### Linux (systemd)
- **Security hardening**: NoNewPrivileges, PrivateTmp, ProtectSystem
- **XDG compliance**: Uses standard directories
- **Resource controls**: Memory limits, file descriptor limits
- **Dependencies**: Starts after network.target

### macOS (launchd)
- **KeepAlive**: Automatic restart on crash
- **ProcessType**: Runs as background process
- **Nice value**: Lower priority to avoid interfering with user tasks
- **Network state awareness**: Can depend on network availability

## Installation

Use the provided scripts in the `scripts/` directory:

### Install Service
```bash
# System-wide installation (requires sudo)
sudo ./scripts/install-service.sh

# User-level installation (no sudo required)
./scripts/install-service.sh --user
```

### Manage Service
```bash
# Start service
./scripts/manage-service.sh start

# Stop service
./scripts/manage-service.sh stop

# Check status
./scripts/manage-service.sh status

# View logs
./scripts/manage-service.sh logs
```

### Uninstall Service
```bash
# Uninstall system service
sudo ./scripts/uninstall-service.sh

# Uninstall user service
./scripts/uninstall-service.sh --user

# Also remove log files
./scripts/uninstall-service.sh --user --clean-logs
```

## File Locations

### Linux
- **System service**: `/etc/systemd/system/scopesd.service`
- **User service**: `~/.config/systemd/user/scopesd.service`
- **Runtime directory**: `$XDG_RUNTIME_DIR/scopes/`
- **Endpoint file**: `$XDG_RUNTIME_DIR/scopes/scopesd.endpoint`
- **Logs**: `journalctl -u scopesd` (system) or `journalctl --user -u scopesd` (user)

### macOS
- **System service**: `/Library/LaunchDaemons/com.github.kamiazya.scopesd.plist`
- **User service**: `~/Library/LaunchAgents/com.github.kamiazya.scopesd.plist`
- **Runtime directory**: `~/Library/Application Support/scopes/run/`
- **Endpoint file**: `~/Library/Application Support/scopes/run/scopesd.endpoint`
- **Logs**: `~/Library/Logs/scopes/scopesd.log`

## Security Considerations

### System-wide Installation
- Runs with limited privileges even when installed system-wide
- Uses OS security features (systemd hardening, launchd sandboxing)
- Binds only to localhost (127.0.0.1)

### User-level Installation
- No elevated privileges required
- Runs in user context with user permissions
- Ideal for development and personal use

## Troubleshooting

### Service won't start
1. Check if the daemon binary exists: `which scopesd`
2. Verify permissions on the binary
3. Check logs for errors
4. Ensure no other instance is running

### Can't connect from CLI
1. Check if service is running: `./scripts/manage-service.sh status`
2. Verify endpoint file exists
3. Check file permissions on endpoint file (should be 0600)
4. Try setting `SCOPESD_ENDPOINT` environment variable manually

### High resource usage
1. Check configured limits in service files
2. Review logs for excessive restarts
3. Consider adjusting resource limits
4. Monitor with system tools (top, Activity Monitor)

## Development Notes

### Testing Service Definitions
```bash
# Validate systemd service
systemd-analyze verify scopesd.service

# Validate launchd plist
plutil -lint com.github.kamiazya.scopesd.plist
```

### Customizing Service Files
- Edit resource limits based on your needs
- Adjust restart policies for your use case
- Modify environment variables as needed
- Consider adding dependencies for your setup
