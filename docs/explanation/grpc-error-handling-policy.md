# gRPC Error Handling Policy

## Overview

This document clarifies the error handling strategy for the Scopes daemon gRPC services, specifically addressing the role of the `Envelope.error` field and the primary error communication channel.

## Decision

The `Envelope.error` field defined in `gateway.proto` is **intentionally unused** in the current implementation. All errors are communicated through gRPC Status with google.rpc details.

## Current Implementation

### Primary Error Channel: gRPC Status

All error handling uses the standard gRPC Status mechanism enhanced with google.rpc details:

```kotlin
// From ErrorDetailsBuilder.kt
fun createStatusException(error: ScopeContractError): io.grpc.StatusException {
    val (status, message) = mapContractErrorToGrpcBasic(error)
    val errorDetails = createErrorDetails(error)
    
    return if (errorDetails.isNotEmpty()) {
        // Create google.rpc.Status with details
        val rpcStatus = com.google.rpc.Status.newBuilder()
            .setCode(status.code.value())
            .setMessage(message)
            .addAllDetails(errorDetails)
            .build()
        
        StatusProto.toStatusException(rpcStatus)
    } else {
        // Fall back to simple StatusException
        io.grpc.StatusException(status.withDescription(message))
    }
}
```

### Structured Error Details

The system provides rich error information using google.rpc types:

- **BadRequest**: For validation errors with field-level details
- **PreconditionFailure**: For business rule violations
- **ResourceInfo**: For not found errors with resource identification
- **Help**: Links to documentation for common errors
- **ErrorInfo**: Categorization with domain, reason, and metadata
- **LocalizedMessage**: For internationalization support

### Client-Side Handling

The CLI client (`GrpcStatusDetailsMapper`) automatically extracts and interprets these details:

```kotlin
// Extracts structured error information from Status
val errorInfo = extractErrorInfo(status)
val badRequest = extractBadRequest(status)
val preconditionFailure = extractPreconditionFailure(status)
// ... maps to ScopeContractError
```

## Rationale

### 1. Standard Compliance
gRPC Status is the standard error channel in the gRPC ecosystem. All gRPC clients and tools understand this mechanism.

### 2. Rich Error Details
google.rpc provides structured, machine-readable error information that enables:
- Programmatic error handling
- Detailed validation feedback
- Internationalization support
- Help links and documentation references

### 3. Consistency
A single error channel prevents ambiguity about where to look for errors. Clients don't need to check both Status and Envelope.error.

### 4. Framework Support
Standard gRPC interceptors, monitoring tools, and libraries work with Status errors automatically.

## Future Considerations

The `Envelope.error` field is reserved for potential future use cases that don't fit the standard error model:

### Partial Success Scenarios
- Batch operations where some items succeed and others fail
- Long-running operations with warnings but overall success
- Streaming responses with per-item errors

### Success with Warnings
- Operations that complete but with caveats
- Deprecation notices on successful calls
- Performance warnings

### Example Future Usage
```proto
// Hypothetical batch response with partial failures
Envelope {
    kind: "BatchCreateScopesResponse"
    payload: {
        succeeded: ["scope1", "scope2"]
        failed: ["scope3"]
    }
    error: {
        code: 0  // Not a failure code
        message: "Partial success: 2 of 3 scopes created"
        details: {
            // Per-item error details
        }
    }
}
```

## Implementation Guidelines

### DO
- Use `ErrorDetailsBuilder` for all error responses
- Provide rich error details using google.rpc types
- Map contract errors to appropriate gRPC status codes
- Include helpful metadata in ErrorInfo

### DON'T
- Don't use `Envelope.error` for regular errors
- Don't implement `createErrorResponse` methods
- Don't return success Status with error data in payload
- Don't lose error context during mapping

### Error Mapping Example
```kotlin
when (error) {
    is ScopeContractError.InputError.InvalidTitle -> {
        // Status: INVALID_ARGUMENT
        // Details: BadRequest with field violation
    }
    is ScopeContractError.BusinessError.NotFound -> {
        // Status: NOT_FOUND
        // Details: ResourceInfo with resource identification
    }
    is ScopeContractError.SystemError.ServiceUnavailable -> {
        // Status: UNAVAILABLE
        // Details: ErrorInfo + Help links
    }
}
```

## Native-to-Native gRPC Limitations (2025-01)

### Known Issue: Protocol Negotiation Failures

**Problem**: Native CLI communicating with native daemon encounters intermittent connection failures.

**Error Pattern**:
```
Status: UNAVAILABLE
Description: io exception
Details: Channel state IDLE, transport negotiation failed
```

**Root Cause**: GraalVM native compilation affects Netty's transport layer, particularly:
- Native epoll/kqueue transport incompatibilities
- gRPC protocol negotiation timing issues
- Netty EventLoop initialization differences

**Workarounds**:
1. **Use JVM CLI with Native Daemon** (Recommended)
   ```bash
   ./scopesd &  # Native daemon
   ./gradlew :apps-scopes:run --args="create 'Task'"  # JVM CLI
   ```

2. **Force NIO Transport** (Experimental)
   ```kotlin
   NettyChannelBuilder.forAddress(host, port)
       .channelType(NioSocketChannel::class.java)
       .eventLoopGroup(NioEventLoopGroup())
   ```

3. **Retry Logic** (Partial mitigation)
   ```kotlin
   suspend fun withRetry(maxAttempts: Int = 3, operation: suspend () -> T): T {
       repeat(maxAttempts - 1) { attempt ->
           try { return operation() }
           catch (e: StatusException) {
               if (e.status.code == Status.Code.UNAVAILABLE) {
                   delay(100 * (attempt + 1))
                   continue
               }
               throw e
           }
       }
       return operation()
   }
   ```

### gRPC-Only CLI Architecture

**Major Change**: CLI converted to pure gRPC client (SQLite eliminated).

**Benefits**:
- ✅ Eliminates SQLite JNI segmentation faults
- ✅ Simplifies native-image configuration
- ✅ Clear separation: daemon handles data, CLI handles interface
- ✅ Consistent behavior across JVM/native builds

**Implications**:
- CLI requires daemon to be running
- No offline operation capability
- All operations go through gRPC TaskGatewayService

### Error Diagnostic Commands

```bash
# Check overall system status
./scopes info

# Test basic connectivity (should always work)
./scopes --help

# Test gRPC operations (may fail native-to-native)
./scopes create "Connection Test"

# Environment debugging
echo $SCOPESD_ENDPOINT
cat /tmp/scopesd-endpoint  # Check endpoint file
```

## Testing Considerations

### Unit Tests
- Verify Status codes match error types
- Ensure error details are properly populated
- Test detail extraction on client side

### Integration Tests
- End-to-end error propagation
- Client interpretation of structured errors
- Error message localization
- **Multi-configuration testing** (JVM CLI, native CLI, native daemon)

### Native Build Testing
```kotlin
@Test
fun `test native-to-native gRPC with fallback`() = runTest {
    val daemon = startNativeDaemon()
    try {
        val result = executeNativeCli("create", "Test Scope")
        if (result.isFailure && isNativeProtocolError(result.error)) {
            // Expected failure - log and continue
            logger.warn("Native-to-native gRPC failed as expected: ${result.error}")
        } else {
            // Unexpected success or different error
            assertThat(result).isSuccess()
        }
    } finally {
        daemon.shutdown()
    }
}
```

## Migration Notes

If we decide to use `Envelope.error` in the future:
1. Document clear distinction between Status errors and Envelope errors
2. Update client code to check both error channels
3. Provide migration guide for existing error handlers
4. Consider backward compatibility

## Related Documentation

- [gRPC Status Codes](https://grpc.io/docs/guides/status-codes/)
- [google.rpc Error Model](https://cloud.google.com/apis/design/errors#error_model)
- [Error Handling Guidelines](../guides/development/error-handling.md)
- [CLI-Daemon Communication](./cli-daemon-grpc.md)
