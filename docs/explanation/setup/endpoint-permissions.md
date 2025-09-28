# Endpoint File Permissions

This document explains how endpoint file permissions work across different operating systems for secure daemon communication.

## Overview

The endpoint file (`scopesd.endpoint`) contains critical information for CLI-daemon communication:
- Daemon address (host:port)
- Process ID (PID)
- Start time

This file requires appropriate permissions to prevent unauthorized access while allowing legitimate communication.

## Platform-Specific Behavior

### Linux and macOS

On Unix-like systems, the endpoint file is created with restricted permissions:

```bash
# File permissions: 0600 (read/write for owner only)
-rw------- 1 user user 128 Jan 28 10:30 ~/.scopes/run/scopesd.endpoint
```

**Implementation details:**
- File mode explicitly set to `0600` using `java.nio.file.attribute.PosixFilePermissions`
- Only the file owner can read or write the endpoint file
- Other users cannot access daemon connection information
- Provides defense against local privilege escalation

### Windows

On Windows, file permissions are handled differently:

**Default behavior:**
- File permissions API (`setPosixFilePermissions`) is a **no-op** on Windows
- Windows NTFS uses Access Control Lists (ACLs) instead of POSIX permissions
- Files created in user directories inherit parent directory ACLs

**Security considerations:**
- User home directories (`%USERPROFILE%`) are protected by default Windows ACLs
- Only the user and administrators have access to files in user directories
- This provides equivalent security to Unix `0600` permissions
- No additional permission configuration is required

**Location:**
```
%USERPROFILE%\.scopes\run\scopesd.endpoint
```

## Endpoint File Format

Regardless of platform, the endpoint file uses a simple key-value format:

```properties
# Daemon endpoint information
addr=127.0.0.1:52345
pid=12345
started=1706123456789
transport=tcp
```

## Security Best Practices

1. **Never share endpoint files** - They contain sensitive connection information
2. **Check file ownership** - Ensure endpoint files are owned by the expected user
3. **Monitor stale files** - The daemon automatically cleans up stale endpoint files
4. **Use localhost binding** - By default, daemon only binds to localhost addresses

## Related Topics

- [Daemon Architecture](../daemon-architecture.md)
- [Security Best Practices](../../security/README.md)
- [CLI-Daemon Communication](../cli-daemon-grpc.md)
