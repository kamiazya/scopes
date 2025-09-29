# Unix Domain Socket (UDS) Implementation Limitations

## Overview

This document describes the limitations encountered when implementing Unix Domain Socket (UDS) support in the Scopes gRPC infrastructure, particularly when building with GraalVM Native Image.

## Implementation Status

### What Was Implemented

1. **UDS Support Infrastructure**:
  - `EndpointResolver` can parse `unix://` addresses
  - `EndpointInfo` supports UDS with `socketPath` property
  - `GrpcClient` detects and attempts to use UDS connections
  - `GrpcServer` has full UDS server implementation
  - Command-line arguments (`--unix-socket`, `--uds`) and environment variables (`SCOPESD_UNIX_SOCKET`)

2. **Channel Creation**:
  - `ChannelBuilder.createUnixSocketChannel()` method for UDS support
  - Attempted to use platform-specific transports (epoll/kqueue)
  - Simplified to use standard NIO transport

### What Doesn't Work

1. **grpc-netty-shaded Limitations**:
  - The shaded JAR doesn't include platform-specific transports (epoll/kqueue)
  - `DomainSocketAddress` with standard NIO requires Java 16+ features
  - Native image builds fail to bind to Unix domain sockets

2. **Native Binary Issues**:
  - IOException when attempting to bind: "Failed to bind to address /tmp/scopesd.sock"
  - The NIO transport in grpc-netty-shaded cannot handle domain sockets properly
  - No way to use platform-specific transports with the shaded dependency

## Technical Details

### The Core Problem

```kotlin
// This doesn't work with grpc-netty-shaded
NettyServerBuilder.forAddress(DomainSocketAddress(socketPath))
    // Standard NIO transport cannot bind to Unix domain sockets
    .build()
```

The issue stems from:
1. `grpc-netty-shaded` packages only the standard Java NIO transport
2. Unix domain socket support requires platform-specific Netty transports:
  - Linux: `io.netty.channel.epoll.EpollServerDomainSocketChannel`
  - macOS: `io.netty.channel.kqueue.KQueueServerDomainSocketChannel`
3. These classes are not included in the shaded JAR

### Attempted Solutions

1. **Platform-Specific Transports**: 
  - Tried to use epoll/kqueue channels
  - Failed: Classes not available in grpc-netty-shaded

2. **Standard NIO with DomainSocketAddress**:
  - Simplified to use default NIO transport
  - Failed: NIO ServerSocketChannel doesn't support domain socket addresses

3. **Java 16+ Unix Domain Socket Support**:
  - Would require upgrading minimum Java version
  - Still wouldn't work with current Netty version in grpc-netty-shaded

## Workarounds and Recommendations

### 1. Continue Using TCP Sockets

For now, TCP sockets remain the most reliable option:
- Works across all platforms
- No additional dependencies needed
- Minimal performance difference for local IPC

### 2. Alternative Approaches (Future)

1. **Use Non-Shaded Dependencies**:
  ```kotlin
  dependencies {
      implementation("io.grpc:grpc-netty")  // Non-shaded
      implementation("io.netty:netty-transport-native-epoll:4.1.x:linux-x86_64")
      implementation("io.netty:netty-transport-native-kqueue:4.1.x:osx-x86_64")
  }
  ```
  - Pros: Full platform-specific transport support
  - Cons: More complex dependency management, potential version conflicts

2. **Named Pipes (Windows)**:
  - Could implement Windows named pipe support separately
  - Would require platform-specific code

3. **Wait for Java Standard Library Support**:
  - Java 16+ has native Unix domain socket support
  - Would need to upgrade minimum Java version
  - Still requires Netty updates to use it

## Current Recommendation

**Use TCP with loopback (127.0.0.1) for local IPC**:
- Reliable and cross-platform
- Minimal performance overhead for local communication
- No additional dependencies or complexity
- Works with GraalVM Native Image

## Code to Remove or Refactor

If UDS support is not pursued further, consider removing:
1. `ChannelBuilder.createUnixSocketChannel()` method
2. Unix socket handling in `GrpcClient` and `GrpcServer`
3. `--unix-socket`/`--uds` command-line options
4. `SCOPESD_UNIX_SOCKET` environment variable support
5. Unix address parsing in `EndpointResolver`

Or keep the infrastructure for future use when dependencies allow proper implementation.

## References

- [Netty Unix Domain Socket Documentation](https://netty.io/wiki/native-transports.html)
- [gRPC Java Unix Domain Socket Example](https://github.com/grpc/grpc-java/tree/master/examples/example-uds)
- [Java 16 Unix Domain Socket Support](https://openjdk.java.net/jeps/380)
