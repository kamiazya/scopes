package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Instant

/**
 * Root sealed class for all infrastructure adapter errors.
 * These errors represent technical failures in external systems and infrastructure components.
 * They should be translated to domain RepositoryError types before being exposed to higher layers.
 * 
 * With Kotlin 1.5+, sealed class subclasses can be defined in separate files within the same package,
 * allowing for better organization and reduced nesting.
 */
sealed class InfrastructureAdapterError {
    abstract val timestamp: Instant
    abstract val correlationId: String?
    
    /**
     * Indicates whether this error represents a transient condition that can be safely retried.
     * 
     * **Retry Semantics:**
     * - `true`: The operation can be retried with reasonable expectation of success
     * - `false`: The operation should NOT be retried as it represents a persistent condition
     * 
     * **Important Notes:**
     * - This flag indicates **immediate** retry potential without mandatory delay
     * - For operations requiring delay (rate limits, circuit breakers), additional timing 
     *   information should be checked (e.g., `resetTime`, `retryAfter`, `nextAttemptAt`)
     * - Callers should implement exponential backoff and maximum retry limits
     * - Transient network issues, resource exhaustion, and deadlocks are typically retryable
     * - Configuration errors, permission issues, and data integrity violations are not retryable
     * 
     * **Example Usage:**
     * ```kotlin
     * if (error.retryable && retryCount < maxRetries) {
     *     delay(exponentialBackoff(retryCount))
     *     retry()
     * } else {
     *     handleFailure(error)
     * }
     * ```
     */
    abstract val retryable: Boolean
}

// Supporting enums and types for database errors
enum class TransactionOperation { COMMIT, ROLLBACK, SAVEPOINT, DEADLOCK }
enum class TransactionIsolationLevel { READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE }
enum class DatabaseResource { CONNECTIONS, MEMORY, STORAGE, LOCKS }

/**
 * Types of query errors for more granular retry logic.
 */
enum class QueryErrorType(val retryable: Boolean) {
    /** SQL syntax errors, table/column not found, etc. - not retryable */
    SYNTAX(false),
    
    /** Constraint violations like primary key, foreign key, check constraints - not retryable */
    CONSTRAINT(false),
    
    /** Query execution timeouts - retryable as they may succeed with more time */
    TIMEOUT(true),
    
    /** Lock wait timeouts during concurrent operations - retryable */
    LOCK_WAIT_TIMEOUT(true),
    
    /** Transient connection issues during query execution - retryable */
    TRANSIENT_CONNECTION(true),
    
    /** Other/unknown query errors - conservatively non-retryable */
    OTHER(false)
}

// Supporting enums and types for external API errors
enum class NetworkErrorType(val retryable: Boolean) {
    DNS_RESOLUTION(true),
    CONNECTION_REFUSED(true), 
    CONNECTION_TIMEOUT(true),
    SSL_HANDSHAKE(false),
    CERTIFICATE_ERROR(false),
    UNKNOWN_HOST(false)
}

enum class CircuitBreakerState { CLOSED, OPEN, HALF_OPEN }
enum class RateLimitType { REQUESTS_PER_MINUTE, REQUESTS_PER_HOUR, BANDWIDTH, CONCURRENT_REQUESTS }
enum class ServiceHealthStatus { HEALTHY, DEGRADED, UNHEALTHY, MAINTENANCE }

// Supporting enums and types for filesystem errors
enum class FileOperation {
    READ, WRITE, CREATE, DELETE, MOVE, COPY;
    
    fun isRetryable(): Boolean = when (this) {
        READ, WRITE -> true
        CREATE, DELETE, MOVE, COPY -> false
    }
}

enum class FilePermission { READ, WRITE, EXECUTE, FULL }
enum class StorageErrorType { DISK_FULL, QUOTA_EXCEEDED, TEMPORARY_FULL }
enum class FileLockType { SHARED, EXCLUSIVE, ADVISORY }

// Supporting enums and types for messaging errors
enum class ConnectionType { PRODUCER, CONSUMER, BIDIRECTIONAL }
enum class SerializationOperation { SERIALIZE, DESERIALIZE }
enum class QueueCapacityType { MESSAGE_COUNT, TOTAL_SIZE, MEMORY_USAGE }

// Supporting enums and types for configuration errors
enum class ConfigurationType {
    FILE, ENVIRONMENT, REMOTE, DATABASE;
    
    fun isRetryable(): Boolean = when (this) {
        REMOTE, DATABASE -> true
        FILE, ENVIRONMENT -> false
    }
}

enum class EncryptionOperation { ENCRYPT, DECRYPT }

// Supporting enums and types for transaction errors
enum class CoordinationType { TWO_PHASE_COMMIT, SAGA, COMPENSATION }
enum class IsolationViolationType { DIRTY_READ, NON_REPEATABLE_READ, PHANTOM_READ }