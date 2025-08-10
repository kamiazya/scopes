package io.github.kamiazya.scopes.infrastructure.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlinx.datetime.Instant

/**
 * Root sealed class for all infrastructure adapter errors.
 * These errors represent technical failures in external systems and infrastructure components.
 * They should be translated to domain RepositoryError types before being exposed to higher layers.
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
    
    /**
     * Database adapter errors for persistence operations.
     * Covers connection pools, queries, transactions, and data integrity issues.
     */
    sealed class DatabaseAdapterError : InfrastructureAdapterError() {
        
        /**
         * Connection-related database errors including pool exhaustion and network issues.
         */
        data class ConnectionError(
            val connectionString: String,
            val poolSize: Int?,
            val activeConnections: Int?,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : DatabaseAdapterError() {
            // Connection issues are typically transient (network blips, pool exhaustion)
            override val retryable: Boolean = true
        }
        
        /**
         * Query execution errors including timeouts and syntax errors.
         */
        data class QueryError(
            val query: String,
            val parameters: Map<String, Any>? = null,
            val executionTimeMs: Long?,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : DatabaseAdapterError() {
            // Most query errors (syntax, invalid references) are persistent conditions
            // Only query timeouts might be retryable, but we classify conservatively as false
            override val retryable: Boolean = false
        }
        
        /**
         * Transaction-related errors including deadlocks and rollback failures.
         */
        data class TransactionError(
            val transactionId: String,
            val operation: TransactionOperation,
            val isolationLevel: TransactionIsolationLevel?,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : DatabaseAdapterError() {
            // Only deadlocks are retryable as they represent transient resource conflicts
            // Other transaction errors (rollback failures, etc.) indicate persistent issues
            override val retryable: Boolean = operation == TransactionOperation.DEADLOCK
        }
        
        /**
         * Data integrity violations including constraint failures and corruption.
         */
        data class DataIntegrityError(
            val table: String,
            val constraint: String?,
            val violatedData: Map<String, Any>?,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : DatabaseAdapterError() {
            // Data integrity violations are persistent conditions that require data correction
            override val retryable: Boolean = false
        }
        
        /**
         * Resource exhaustion errors including storage and memory limits.
         */
        data class ResourceExhaustionError(
            val resource: DatabaseResource,
            val currentUsage: Long,
            val maxCapacity: Long,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : DatabaseAdapterError() {
            // Resource exhaustion is often temporary (connections freed, memory reclaimed)
            override val retryable: Boolean = true
        }
    }
    
    /**
     * HTTP/External API adapter errors for service integration.
     * Covers network issues, service unavailability, and circuit breaker states.
     */
    sealed class ExternalApiAdapterError : InfrastructureAdapterError() {
        
        /**
         * Network-level connectivity errors.
         */
        data class NetworkError(
            val endpoint: String,
            val method: String,
            val networkType: NetworkErrorType,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ExternalApiAdapterError() {
            override val retryable: Boolean = networkType.retryable
        }
        
        /**
         * HTTP-specific errors including status codes and protocol issues.
         */
        data class HttpError(
            val endpoint: String,
            val method: String,
            val statusCode: Int,
            val responseBody: String?,
            val headers: Map<String, String>?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ExternalApiAdapterError() {
            // 5xx server errors and 429 rate limits are typically retryable
            // 4xx client errors (except 429) indicate persistent request issues
            override val retryable: Boolean = statusCode in 500..599 || statusCode == 429
        }
        
        /**
         * Service timeout errors for slow or unresponsive services.
         */
        data class TimeoutError(
            val endpoint: String,
            val method: String,
            val timeoutMs: Long,
            val elapsedMs: Long,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ExternalApiAdapterError() {
            override val retryable: Boolean = true
        }
        
        /**
         * Circuit breaker errors when service is in open state.
         */
        data class CircuitBreakerError(
            val endpoint: String,
            val state: CircuitBreakerState,
            val failureCount: Int,
            val nextAttemptAt: Instant?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ExternalApiAdapterError() {
            override val retryable: Boolean = state == CircuitBreakerState.HALF_OPEN
        }
        
        /**
         * Rate limiting errors when API limits are exceeded.
         */
        data class RateLimitError(
            val endpoint: String,
            val method: String,
            val limitType: RateLimitType,
            val resetTime: Instant?,
            val remainingQuota: Long?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ExternalApiAdapterError() {
            // Rate limits are retryable when resetTime is provided, indicating when to retry
            // Without resetTime, we don't know when the limit resets
            override val retryable: Boolean = resetTime != null
        }
        
        /**
         * Service unavailability errors.
         */
        data class ServiceUnavailableError(
            val endpoint: String,
            val serviceName: String,
            val healthStatus: ServiceHealthStatus?,
            val retryAfter: Instant?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ExternalApiAdapterError() {
            override val retryable: Boolean = retryAfter != null
        }
    }
    
    /**
     * Filesystem adapter errors for file and directory operations.
     * Covers I/O operations, permissions, and storage issues.
     */
    sealed class FileSystemAdapterError : InfrastructureAdapterError() {
        
        /**
         * File I/O operation errors.
         */
        data class IOError(
            val path: String,
            val operation: FileOperation,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : FileSystemAdapterError() {
            override val retryable: Boolean = operation.isRetryable()
        }
        
        /**
         * Permission and access control errors.
         */
        data class PermissionError(
            val path: String,
            val operation: FileOperation,
            val requiredPermission: FilePermission,
            val currentPermission: FilePermission?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : FileSystemAdapterError() {
            override val retryable: Boolean = false
        }
        
        /**
         * Storage space and quota errors.
         */
        data class StorageError(
            val path: String,
            val storageType: StorageErrorType,
            val availableSpace: Long?,
            val requiredSpace: Long?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : FileSystemAdapterError() {
            override val retryable: Boolean = storageType == StorageErrorType.TEMPORARY_FULL
        }
        
        /**
         * File locking and concurrent access errors.
         */
        data class ConcurrencyError(
            val path: String,
            val operation: FileOperation,
            val lockType: FileLockType,
            val holdingProcess: String?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : FileSystemAdapterError() {
            override val retryable: Boolean = true
        }
    }
    
    /**
     * Messaging adapter errors for event bus and queue operations.
     * Covers message delivery, serialization, and broker connectivity.
     */
    sealed class MessagingAdapterError : InfrastructureAdapterError() {
        
        /**
         * Message broker connection errors.
         */
        data class BrokerConnectionError(
            val brokerUrl: String,
            val connectionType: ConnectionType,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : MessagingAdapterError() {
            override val retryable: Boolean = true
        }
        
        /**
         * Message serialization/deserialization errors.
         */
        data class SerializationError(
            val messageType: String,
            val operation: SerializationOperation,
            val payloadSize: Long?,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : MessagingAdapterError() {
            override val retryable: Boolean = false
        }
        
        /**
         * Message delivery failures.
         */
        data class DeliveryError(
            val messageId: String,
            val destination: String,
            val deliveryAttempt: Int,
            val maxRetries: Int,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : MessagingAdapterError() {
            override val retryable: Boolean = deliveryAttempt < maxRetries
        }
        
        /**
         * Queue/Topic capacity and resource errors.
         */
        data class CapacityError(
            val queueName: String,
            val capacityType: QueueCapacityType,
            val currentSize: Long,
            val maxCapacity: Long,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : MessagingAdapterError() {
            override val retryable: Boolean = true
        }
    }
    
    /**
     * Configuration adapter errors for application settings and environment variables.
     * Covers loading, validation, and environment-specific issues.
     */
    sealed class ConfigurationAdapterError : InfrastructureAdapterError() {
        
        /**
         * Configuration source loading errors.
         */
        data class LoadingError(
            val source: String,
            val configType: ConfigurationType,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ConfigurationAdapterError() {
            override val retryable: Boolean = configType.isRetryable()
        }
        
        /**
         * Configuration validation errors.
         */
        data class ValidationError(
            val configKey: String,
            val expectedType: String,
            val actualValue: String?,
            val validationRule: String,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ConfigurationAdapterError() {
            override val retryable: Boolean = false
        }
        
        /**
         * Environment-specific configuration errors.
         */
        data class EnvironmentError(
            val environment: String,
            val missingKeys: List<String>,
            val invalidKeys: Map<String, String>,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ConfigurationAdapterError() {
            override val retryable: Boolean = false
        }
        
        /**
         * Configuration encryption/decryption errors.
         */
        data class EncryptionError(
            val configKey: String,
            val operation: EncryptionOperation,
            val keyId: String?,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : ConfigurationAdapterError() {
            override val retryable: Boolean = false
        }
    }
    
    /**
     * Transaction manager adapter errors for distributed transactions.
     * Covers rollback, isolation, and coordination issues.
     */
    sealed class TransactionAdapterError : InfrastructureAdapterError() {
        
        /**
         * Transaction coordination errors in distributed systems.
         */
        data class CoordinationError(
            val transactionId: String,
            val participantCount: Int,
            val failedParticipants: List<String>,
            val coordinationType: CoordinationType,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : TransactionAdapterError() {
            override val retryable: Boolean = coordinationType == CoordinationType.SAGA
        }
        
        /**
         * Transaction rollback errors.
         */
        data class RollbackError(
            val transactionId: String,
            val rollbackCause: String,
            val partialRollback: Boolean,
            val affectedResources: List<String>,
            val cause: Throwable,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : TransactionAdapterError() {
            override val retryable: Boolean = partialRollback
        }
        
        /**
         * Transaction isolation level violations.
         */
        data class IsolationError(
            val transactionId: String,
            val isolationLevel: TransactionIsolationLevel,
            val violationType: IsolationViolationType,
            val conflictingTransactionId: String?,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : TransactionAdapterError() {
            override val retryable: Boolean = violationType == IsolationViolationType.DIRTY_READ
        }
        
        /**
         * Transaction timeout errors.
         */
        data class TimeoutError(
            val transactionId: String,
            val timeoutMs: Long,
            val elapsedMs: Long,
            val operationsCompleted: Int,
            val operationsRemaining: Int,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : TransactionAdapterError() {
            override val retryable: Boolean = false
        }
        
        /**
         * Transaction deadlock detection errors.
         */
        data class DeadlockError(
            val transactionId: String,
            val deadlockChain: List<String>,
            val resourcesInvolved: List<String>,
            val detectionTimeMs: Long,
            override val timestamp: Instant,
            override val correlationId: String? = null
        ) : TransactionAdapterError() {
            override val retryable: Boolean = true
        }
    }
}

// Supporting enums and types for database errors
enum class TransactionOperation { COMMIT, ROLLBACK, SAVEPOINT, DEADLOCK }
enum class TransactionIsolationLevel { READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE }
enum class DatabaseResource { CONNECTIONS, MEMORY, STORAGE, LOCKS }

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