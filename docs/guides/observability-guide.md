# Observability Guide

## Overview

Scopes includes a comprehensive observability infrastructure (`platform/observability/`) that provides structured logging, metrics, and tracing capabilities for monitoring and debugging the application in production environments.

## Structured Logging

### Logger Interface

The system provides a type-safe, structured logging API:

```kotlin
interface StructuredLogger {
    fun debug(message: String, context: Map<String, LogValue> = emptyMap())
    fun info(message: String, context: Map<String, LogValue> = emptyMap())
    fun warn(message: String, context: Map<String, LogValue> = emptyMap())
    fun error(message: String, error: Throwable? = null, context: Map<String, LogValue> = emptyMap())
}
```

### LogValue Type System

Type-safe log values ensure consistent serialization:

```kotlin
sealed class LogValue {
    data class StringValue(val value: String) : LogValue()
    data class NumberValue(val value: Number) : LogValue()
    data class BooleanValue(val value: Boolean) : LogValue()
    data class ObjectValue(val value: Map<String, LogValue>) : LogValue()
    data class ArrayValue(val value: List<LogValue>) : LogValue()
    object NullValue : LogValue()
}
```

### Usage Examples

```kotlin
class ScopeService(private val logger: StructuredLogger) {
    suspend fun createScope(command: CreateScopeCommand) {
        logger.info(
            "Creating scope",
            mapOf(
                "scope.title" to command.title.toLogValue(),
                "scope.parent" to command.parentId?.toLogValue(),
                "user.id" to currentUser.id.toLogValue()
            )
        )

        try {
            // ... business logic
            logger.info(
                "Scope created successfully",
                mapOf(
                    "scope.id" to newScope.id.toLogValue(),
                    "duration.ms" to duration.toLogValue()
                )
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to create scope",
                e,
                mapOf(
                    "error.type" to e::class.simpleName.toLogValue(),
                    "scope.title" to command.title.toLogValue()
                )
            )
        }
    }
}
```

## Application Information

### ApplicationInfo Class

Track application instance metadata:

```kotlin
data class ApplicationInfo(
    val name: String,
    val version: String,
    val type: ApplicationType,
    val startTime: Instant = Clock.System.now(),
    val instanceId: String = generateInstanceId(),
    val customMetadata: Map<String, Any> = emptyMap()
)

enum class ApplicationType {
    CLI,
    MCP_SERVER,
    DAEMON,
    TEST
}
```

### Automatic Context Enrichment

All log messages are automatically enriched with:
- Application name and version
- Instance ID for correlation
- Timestamp with microsecond precision
- Thread/coroutine context
- Request/trace IDs when available

## Log Levels and Configuration

### Log Level Hierarchy

```kotlin
enum class LogLevel {
    TRACE,  // Detailed execution flow
    DEBUG,  // Diagnostic information
    INFO,   // General information
    WARN,   // Warning conditions
    ERROR,  // Error conditions
    FATAL   // Critical failures
}
```

### Configuration

Configure logging via environment variables:

```bash
# Set log level
export SCOPES_LOG_LEVEL=DEBUG

# Enable structured JSON output
export SCOPES_LOG_FORMAT=json

# Set log output destination
export SCOPES_LOG_OUTPUT=stdout  # stdout, stderr, file

# Enable correlation IDs
export SCOPES_LOG_CORRELATION=true
```

## Metrics Collection

### Metric Types

```kotlin
interface MetricsCollector {
    // Counters - monotonically increasing values
    fun incrementCounter(name: String, tags: Map<String, String> = emptyMap())

    // Gauges - point-in-time values
    fun recordGauge(name: String, value: Double, tags: Map<String, String> = emptyMap())

    // Histograms - distribution of values
    fun recordHistogram(name: String, value: Double, tags: Map<String, String> = emptyMap())

    // Timers - measure duration
    suspend fun <T> measureTime(name: String, block: suspend () -> T): T
}
```

### Standard Metrics

The system automatically collects:

#### Application Metrics
- `app.start.time` - Application startup time
- `app.memory.used` - Memory usage
- `app.threads.count` - Active thread count

#### Business Metrics
- `scope.created.count` - Number of scopes created
- `scope.operation.duration` - Operation latencies
- `alias.resolution.time` - Alias lookup performance

#### Database Metrics
- `db.connection.count` - Active connections
- `db.query.duration` - Query execution time
- `db.transaction.count` - Transaction throughput

## Tracing

### Distributed Tracing Support

```kotlin
interface TracingContext {
    val traceId: String
    val spanId: String
    val parentSpanId: String?

    fun createChildSpan(name: String): TracingContext
    fun addAttribute(key: String, value: String)
    fun addEvent(name: String, attributes: Map<String, String> = emptyMap())
}
```

### Trace Propagation

Traces are automatically propagated through:
- HTTP headers (W3C Trace Context)
- MCP protocol messages
- Event sourcing events
- Background jobs

## Error Tracking

### Error Classification

```kotlin
sealed class ErrorCategory {
    object ValidationError : ErrorCategory()
    object BusinessRuleViolation : ErrorCategory()
    object InfrastructureError : ErrorCategory()
    object ExternalServiceError : ErrorCategory()
    object UnexpectedError : ErrorCategory()
}
```

### Error Context Collection

When errors occur, the system automatically captures:
- Stack traces with source mapping
- Request/command context
- User context (if available)
- System state snapshot
- Recent log entries

## Performance Monitoring

### Operation Timing

```kotlin
class PerformanceMonitor {
    suspend fun <T> monitor(
        operationName: String,
        threshold: Duration = 1.seconds
    ): T {
        val start = Clock.System.now()
        try {
            return operation()
        } finally {
            val duration = Clock.System.now() - start
            if (duration > threshold) {
                logger.warn(
                    "Slow operation detected",
                    mapOf(
                        "operation" to operationName.toLogValue(),
                        "duration.ms" to duration.inWholeMilliseconds.toLogValue(),
                        "threshold.ms" to threshold.inWholeMilliseconds.toLogValue()
                    )
                )
            }
        }
    }
}
```

## Integration with External Systems

### Export Formats

The observability layer supports multiple export formats:

#### JSON Lines (Default)
```json
{"timestamp":"2025-01-18T10:30:45.123Z","level":"INFO","message":"Scope created","scope.id":"01HXY...","duration.ms":45}
```

#### OpenTelemetry Protocol (OTLP)
Compatible with OpenTelemetry collectors for integration with:
- Jaeger (tracing)
- Prometheus (metrics)
- Elasticsearch (logs)

### Log Aggregation

Configure log shipping to external systems:

```yaml
observability:
  exporters:
    - type: otlp
      endpoint: http://collector:4317
    - type: elasticsearch
      endpoint: http://elastic:9200
      index: scopes-logs
```

## Debugging Features

### Debug Mode

Enable detailed debugging output:

```kotlin
// Enable via environment variable
export SCOPES_DEBUG=true

// Or programmatically
Logger.setDebugMode(true)
```

Debug mode enables:
- Verbose SQL query logging
- HTTP request/response dumps
- Event sourcing details
- MCP protocol messages

### Correlation IDs

Track requests across system boundaries:

```kotlin
class CorrelationContext {
    companion object {
        fun current(): String =
            ThreadLocal.get() ?: UUID.randomUUID().toString()

        fun withCorrelationId(id: String, block: () -> Unit) {
            ThreadLocal.set(id)
            try {
                block()
            } finally {
                ThreadLocal.remove()
            }
        }
    }
}
```

## Best Practices

### 1. Structured Context

Always prefer structured context over string interpolation:

```kotlin
// ❌ Bad
logger.info("Created scope ${scope.id} with title ${scope.title}")

// ✅ Good
logger.info(
    "Scope created",
    mapOf(
        "scope.id" to scope.id.toLogValue(),
        "scope.title" to scope.title.toLogValue()
    )
)
```

### 2. Consistent Naming

Use dot notation for nested context:
- `scope.id`, `scope.title`
- `user.id`, `user.email`
- `error.type`, `error.message`

### 3. Performance Impact

Be mindful of logging overhead:

```kotlin
// Use lazy evaluation for expensive operations
logger.debug("Expensive computation") {
    mapOf("result" to expensiveComputation().toLogValue())
}
```

### 4. Security Considerations

Never log sensitive information:

```kotlin
// Sanitize sensitive data
logger.info(
    "User authenticated",
    mapOf(
        "user.id" to user.id.toLogValue(),
        "user.email" to user.email.mask().toLogValue()
        // Never log: passwords, tokens, credit cards
    )
)
```

## Monitoring Dashboards

### Sample Queries

#### Find slow operations
```sql
SELECT operation, percentile(duration_ms, 0.95) as p95
FROM logs
WHERE level = 'WARN' AND message LIKE 'Slow operation%'
GROUP BY operation
```

#### Error rate by type
```sql
SELECT error_type, COUNT(*) as count
FROM logs
WHERE level = 'ERROR'
GROUP BY error_type
ORDER BY count DESC
```

#### User activity timeline
```sql
SELECT timestamp, user_id, operation
FROM logs
WHERE user_id IS NOT NULL
ORDER BY timestamp
```

## Troubleshooting

### Common Issues

#### Missing Logs
1. Check log level configuration
2. Verify logger initialization
3. Ensure output destination is writable

#### Performance Degradation
1. Reduce log level in production
2. Enable asynchronous logging
3. Use sampling for high-volume operations

#### Log Correlation Issues
1. Verify correlation ID propagation
2. Check trace context headers
3. Ensure consistent timestamp formats

## Related Documentation

<!-- Future documentation:
- [Error Handling Guidelines](../guidelines/error-handling.md) - Error handling patterns
- [Performance Guidelines](../guidelines/performance.md) - Performance optimization
- [Security Best Practices](../guidelines/security.md) - Security considerations
- [Testing Guide](../guides/testing-guide.md) - Testing observability
-->
