# CLI↔Daemon gRPC Architecture (v1beta)

This document defines the architecture and design of the gRPC-based local IPC between the Scopes CLI and the background daemon. It follows our Clean Architecture principles and Diátaxis documentation approach.

Related docs:
- Clean Architecture: ./clean-architecture.md
- Dependency Rules: ./dependency-rules.md
- Security: ../security/dependency-security.md
- Setup (GraalVM, packaging): ../setup/README.md, ../setup/graalvm-setup.md
- Development guidelines: ../guides/development/README.md, ../guides/observability-guide.md

## Why gRPC now
- Separation of concerns: keep CLI thin; move long-running/stateful work to daemon.
- Future needs: streaming logs/progress, structured errors, transport evolution (UDS/Pipes).
- Standard tooling: mature ecosystem, codegen, interceptors, observability.

## Scope and Non-goals
- In scope:
  - Control endpoints (Ping, GetVersion, Shutdown) for health and operations.
  - A generic gateway (Envelope) that proxies selected commands and queries without exposing domain DTOs.
  - Local loopback TCP with plaintext; endpoint discovery via file for portability.
  - **gRPC-only CLI client** (SQLite dependencies completely eliminated).
- Out of scope (for v1beta):
  - Remote access across hosts, multi-tenant isolation, or long-running job orchestration.
  - Native-to-native gRPC reliability (protocol negotiation issues remain).

## Schema and Versioning
- Package: `scopes.daemon.v1beta` (breaking changes allowed during 0.x).
- Services:
  - ControlService: Ping, GetVersion, Shutdown.
  - TaskGatewayService: ExecuteCommand, Query (streaming later).
- DTO strategy: do not expose domain DTOs directly; use `Envelope { kind, version, idempotency_key, headers, payload }` and map in adapters.

Proto sources:
- interfaces/rpc-contracts/src/main/proto/scopes/daemon/v1beta/control.proto
- interfaces/rpc-contracts/src/main/proto/scopes/daemon/v1beta/gateway.proto

## Module Layout
- interfaces/rpc-contracts: neutral .proto and generated stubs (no domain).
- interfaces/daemon-grpc: inbound adapter (gRPC server implementations).
- interfaces/cli-grpc: outbound adapter (gRPC clients/wrappers).
- apps/scopesd: daemon entrypoint; wires infrastructure and starts gRPC server.
- apps/scopes: CLI entrypoint; will optionally call gRPC via a transport toggle.

Dependency direction (enforced by tests/guidelines):
- apps → interfaces → contracts → contexts (application → domain)
- interfaces/daemon-grpc depends up to application; infrastructure wiring occurs in apps/scopesd.

## Transport and Endpoint Discovery
- Transport: TCP on 127.0.0.1 with an ephemeral port (`:0`).
- Endpoint file (permissions 0600):
  - Linux: `$XDG_RUNTIME_DIR/scopes/scopesd.endpoint`
  - macOS: `~/Library/Application Support/scopes/run/scopesd.endpoint`
  - Windows: `%LOCALAPPDATA%/scopes/run/scopesd.endpoint`
  - Windows fallback: if `LOCALAPPDATA` is not set, fall back to `%APPDATA%` and then the user home directory.
- Format (KV):
  - Required: `version`, `addr`, `transport`
  - Optional: `pid`, `started`
  - Example:
    ```
    version=1
    addr=127.0.0.1:52345
    transport=tcp
    pid=12345
    started=1732590635123
    ```
- Planned transports: Unix Domain Socket (Linux/macOS), Named Pipe (Windows).
  - **UDS Implementation Status**: Infrastructure code implemented but not functional due to grpc-netty-shaded limitations (see unix-domain-socket-limitations.md).

## Control Semantics
- Ping: health check and uptime information (`pid`, `uptime_seconds`, `server_time`).
- GetVersion: returns `app_version`, `api_version` (v1beta), `git_revision` (optional) and `build_platform`.
- Shutdown: returns ACK immediately and triggers graceful shutdown asynchronously via the daemon's lifecycle monitor.

## Security and Observability
- Local-only plaintext for MVP; no remote exposure.
- Error policy: gRPC status + details is the primary error channel. The gateway may also include `Envelope.error` in responses to carry structured details; clients must still honor the gRPC status.
- Netty is shaded (`grpc-netty-shaded`) to avoid dependency conflicts; root build pins `netty-codec-http2`.
- Interceptors for correlation IDs and request summaries are implemented on the server. Each RPC logs method, status, latency, and correlation ID.

## Retry Policy
- **Exponential Backoff**: Connection and operation retries use exponential backoff with configurable parameters
- **Default Configuration**: 3 attempts, 1s initial delay, 2x backoff multiplier, 15s max delay
- **Retryable Errors**: UNAVAILABLE, DEADLINE_EXCEEDED, ABORTED, INTERNAL, UNKNOWN, and network errors
- **Environment Variables**:
  - `SCOPES_GRPC_RETRY_MAX_ATTEMPTS`: Maximum retry attempts (default: 3)
  - `SCOPES_GRPC_RETRY_INITIAL_DELAY_MS`: Initial retry delay in milliseconds (default: 1000)
  - `SCOPES_GRPC_RETRY_MAX_DELAY_MS`: Maximum retry delay in milliseconds (default: 15000)
  - `SCOPES_GRPC_RETRY_BACKOFF_MULTIPLIER`: Backoff multiplier (default: 2.0)
- **Per-Operation Control**: Retry can be disabled for specific operations (e.g., shutdown requests)

## CLI Integration
- The CLI provides `scopes info` (docker-like) to display both client-side and daemon-side information.
- Resolution order when connecting to the daemon:
  1. `SCOPESD_ENDPOINT` environment variable (e.g., `127.0.0.1:52345`)
  2. Platform endpoint file (see above)
- Typical output includes server status (Running/Not running), address, pid, uptime, API version, and platform.
- For early Gateway trials, setting `SCOPES_TRANSPORT=grpc` will route certain commands (e.g., `scopes create`) through the TaskGatewayService over gRPC. Without this flag, commands run locally as before.
- **UDS Configuration (Non-functional)**: The following options exist in the code but do not work due to grpc-netty-shaded limitations:
  - Server: `--unix-socket` or `--uds` flag, `SCOPESD_UNIX_SOCKET` environment variable
  - Client: Automatically detects `unix://` addresses in endpoint resolution
  - See unix-domain-socket-limitations.md for details

## Change Management and Compatibility
- Schema version: `v1beta` within proto package; breaking changes may occur during 0.x.
- Backward compatibility: we minimize changes and gate new transport behavior behind configuration flags.
- Communication: changes are recorded in CHANGELOG and documented in reference/guides as needed.

## Developer Notes
- Version catalog: gradle/libs.versions.toml provides grpc, protobuf, grpc-kotlin, and plugin aliases.
- rpc-contracts uses the protobuf Gradle plugin to generate Kotlin/Java stubs; avoid manual source-set wiring where possible.
- Keep adapters thin; map CLI inputs to contract ports via gateway mappers.

## Current Implementation Status (2025-01)

### ✅ Working Configurations
1. **JVM CLI ↔ Native Daemon** (Production Ready)
  - Fully functional and reliable
  - Performance: ~0.3-0.5s command execution
  - Recommended for development and production use

2. **Native CLI Basic Operations** (Limited)
  - Help and simple commands work
  - Builds without SQLite segmentation faults
  - Native CLI is now a pure gRPC client (no local database)

### ⚠️ Known Limitations
1. **Native CLI ↔ Native Daemon** (Protocol Issues)
  - Intermittent connection failures due to Netty native transport incompatibility
  - Error: `UNAVAILABLE: io exception` during protocol negotiation
  - Root cause: GraalVM native compilation affects gRPC/Netty behavior

2. **Unix Domain Socket (UDS) Support** (Non-functional)
  - Implementation code exists but doesn't work with grpc-netty-shaded
  - grpc-netty-shaded only includes standard NIO transport, not platform-specific transports
  - Platform-specific transports (epoll/kqueue) required for UDS are not in shaded JAR
  - See unix-domain-socket-limitations.md for technical details

### 🏗️ Architecture Changes Made
1. **CLI Converted to gRPC-Only Client**
  - Eliminated all SQLite dependencies from CLI build
  - Removed SQLite JNI segmentation faults completely
  - CLI now communicates exclusively via gRPC TaskGatewayService
  - Simplified native-image configuration (no SQLite reflection needed)

2. **Dependency Injection Streamlined**
  - Removed conditional transport logic from CLI
  - Direct injection of GrpcTransport in CliAppModule
  - No fallback to local adapters (daemon required)

### 📁 Key Files Modified
- `apps/scopes/build.gradle.kts`: Removed SQLite dependencies
- `apps/scopes/src/main/resources/META-INF/native-image/native-image.properties`: Cleaned SQLite configs  
- `apps/scopes/src/main/kotlin/io/github/kamiazya/scopes/apps/cli/di/CliAppModule.kt`: gRPC-only DI
- `interfaces/grpc-client-daemon/src/main/kotlin/.../di/grpcClientModule.kt`: New gRPC client module

## FAQ
- Why not expose domain types directly in proto?
  - We are in 0.x with frequent changes; Envelope isolates domain evolution and stabilizes the transport surface.
- Will this work with GraalVM native?
  - **UPDATE**: Yes, with limitations. Native CLI builds successfully and works for basic operations. Native-to-native gRPC has protocol negotiation issues, but JVM CLI with native daemon works perfectly.
- Why was the CLI converted to gRPC-only?
  - SQLite JNI caused persistent segmentation faults in native CLI builds. Converting to gRPC-only eliminated these issues while providing a cleaner architecture.
- What's the recommended production setup?
  - Use JVM CLI with native daemon for maximum reliability and performance.
- Why doesn't UDS work?
  - grpc-netty-shaded doesn't include platform-specific Netty transports (epoll/kqueue) required for Unix domain sockets.
- What are the options to fix native-to-native and UDS issues?
  - Switch from grpc-netty-shaded to non-shaded dependencies (complexity tradeoff)
  - Wait for Java 16+ UDS support and updated Netty/gRPC versions
  - Use alternative IPC mechanisms (REST API, message queues)
  - Keep current setup (JVM CLI + Native daemon works well)
