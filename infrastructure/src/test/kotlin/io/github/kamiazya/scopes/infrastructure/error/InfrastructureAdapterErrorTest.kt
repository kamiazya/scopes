package io.github.kamiazya.scopes.infrastructure.error

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class InfrastructureAdapterErrorTest : StringSpec({

    val testTimestamp = Clock.System.now()
    val testCorrelationId = "test-correlation-123"

    "DatabaseAdapterError should have correct retryability" {
        val connectionError = InfrastructureAdapterError.DatabaseAdapterError.ConnectionError(
            connectionString = "jdbc:postgresql://localhost:5432/test",
            poolSize = 10,
            activeConnections = 10,
            cause = RuntimeException("Connection refused"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        connectionError.retryable shouldBe true
        
        val queryError = InfrastructureAdapterError.DatabaseAdapterError.QueryError(
            query = "SELECT * FROM invalid_table",
            parameters = mapOf("id" to 123),
            executionTimeMs = 1000,
            cause = RuntimeException("Table not found"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        queryError.retryable shouldBe false
        
        val deadlockError = InfrastructureAdapterError.DatabaseAdapterError.TransactionError(
            transactionId = "tx-123",
            operation = TransactionOperation.DEADLOCK,
            isolationLevel = TransactionIsolationLevel.SERIALIZABLE,
            cause = RuntimeException("Deadlock detected"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        deadlockError.retryable shouldBe true
        
        val commitError = InfrastructureAdapterError.DatabaseAdapterError.TransactionError(
            transactionId = "tx-456",
            operation = TransactionOperation.COMMIT,
            isolationLevel = TransactionIsolationLevel.READ_COMMITTED,
            cause = RuntimeException("Commit failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        commitError.retryable shouldBe false
    }

    "ExternalApiAdapterError should have correct retryability based on error type" {
        val networkTimeoutError = InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
            endpoint = "https://api.external.com/users",
            method = "GET",
            networkType = NetworkErrorType.CONNECTION_TIMEOUT,
            cause = RuntimeException("Connection timeout"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        networkTimeoutError.retryable shouldBe true
        
        val sslError = InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
            endpoint = "https://api.external.com/users",
            method = "GET",
            networkType = NetworkErrorType.SSL_HANDSHAKE,
            cause = RuntimeException("SSL handshake failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        sslError.retryable shouldBe false
        
        val serverError = InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
            endpoint = "https://api.external.com/users",
            method = "POST",
            statusCode = 500,
            responseBody = "Internal Server Error",
            headers = mapOf("Content-Type" to "text/plain"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        serverError.retryable shouldBe true
        
        val clientError = InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
            endpoint = "https://api.external.com/users",
            method = "POST",
            statusCode = 400,
            responseBody = "Bad Request",
            headers = mapOf("Content-Type" to "application/json"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        clientError.retryable shouldBe false
        
        val rateLimitError = InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
            endpoint = "https://api.external.com/users",
            method = "GET",
            statusCode = 429,
            responseBody = "Rate limit exceeded",
            headers = mapOf("Retry-After" to "60"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        rateLimitError.retryable shouldBe true
        
        val circuitBreakerOpenError = InfrastructureAdapterError.ExternalApiAdapterError.CircuitBreakerError(
            endpoint = "https://api.external.com/users",
            state = CircuitBreakerState.OPEN,
            failureCount = 5,
            nextAttemptAt = testTimestamp.plus(1.minutes),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        circuitBreakerOpenError.retryable shouldBe false
        
        val circuitBreakerHalfOpenError = InfrastructureAdapterError.ExternalApiAdapterError.CircuitBreakerError(
            endpoint = "https://api.external.com/users",
            state = CircuitBreakerState.HALF_OPEN,
            failureCount = 2,
            nextAttemptAt = testTimestamp,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        circuitBreakerHalfOpenError.retryable shouldBe true
    }

    "FileSystemAdapterError should have correct retryability based on operation and error type" {
        val readIOError = InfrastructureAdapterError.FileSystemAdapterError.IOError(
            path = "/tmp/config.json",
            operation = FileOperation.READ,
            cause = RuntimeException("File temporarily locked"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        readIOError.retryable shouldBe true
        
        val deleteIOError = InfrastructureAdapterError.FileSystemAdapterError.IOError(
            path = "/tmp/temp.txt",
            operation = FileOperation.DELETE,
            cause = RuntimeException("File not found"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        deleteIOError.retryable shouldBe false
        
        val permissionError = InfrastructureAdapterError.FileSystemAdapterError.PermissionError(
            path = "/etc/config.conf",
            operation = FileOperation.WRITE,
            requiredPermission = FilePermission.WRITE,
            currentPermission = FilePermission.READ,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        permissionError.retryable shouldBe false
        
        val diskFullError = InfrastructureAdapterError.FileSystemAdapterError.StorageError(
            path = "/var/log/app.log",
            storageType = StorageErrorType.DISK_FULL,
            availableSpace = 0,
            requiredSpace = 1024,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        diskFullError.retryable shouldBe false
        
        val temporaryFullError = InfrastructureAdapterError.FileSystemAdapterError.StorageError(
            path = "/tmp/cache",
            storageType = StorageErrorType.TEMPORARY_FULL,
            availableSpace = 100,
            requiredSpace = 1024,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        temporaryFullError.retryable shouldBe true
        
        val concurrencyError = InfrastructureAdapterError.FileSystemAdapterError.ConcurrencyError(
            path = "/shared/data.db",
            operation = FileOperation.WRITE,
            lockType = FileLockType.EXCLUSIVE,
            holdingProcess = "process-456",
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        concurrencyError.retryable shouldBe true
    }

    "MessagingAdapterError should have correct retryability based on error context" {
        val brokerConnectionError = InfrastructureAdapterError.MessagingAdapterError.BrokerConnectionError(
            brokerUrl = "amqp://localhost:5672",
            connectionType = ConnectionType.PRODUCER,
            cause = RuntimeException("Connection refused"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        brokerConnectionError.retryable shouldBe true
        
        val serializationError = InfrastructureAdapterError.MessagingAdapterError.SerializationError(
            messageType = "ScopeCreatedEvent",
            operation = SerializationOperation.SERIALIZE,
            payloadSize = 512,
            cause = RuntimeException("Invalid JSON format"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        serializationError.retryable shouldBe false
        
        val retryableDeliveryError = InfrastructureAdapterError.MessagingAdapterError.DeliveryError(
            messageId = "msg-123",
            destination = "scope.events",
            deliveryAttempt = 2,
            maxRetries = 5,
            cause = RuntimeException("Temporary delivery failure"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        retryableDeliveryError.retryable shouldBe true
        
        val maxRetriesDeliveryError = InfrastructureAdapterError.MessagingAdapterError.DeliveryError(
            messageId = "msg-456",
            destination = "scope.events",
            deliveryAttempt = 5,
            maxRetries = 5,
            cause = RuntimeException("Max retries exceeded"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        maxRetriesDeliveryError.retryable shouldBe false
        
        val capacityError = InfrastructureAdapterError.MessagingAdapterError.CapacityError(
            queueName = "scope.events",
            capacityType = QueueCapacityType.MESSAGE_COUNT,
            currentSize = 10000,
            maxCapacity = 10000,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        capacityError.retryable shouldBe true
    }

    "ConfigurationAdapterError should have correct retryability based on configuration type and error" {
        val remoteConfigError = InfrastructureAdapterError.ConfigurationAdapterError.LoadingError(
            source = "https://config.service.com/app-config",
            configType = ConfigurationType.REMOTE,
            cause = RuntimeException("Service unavailable"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        remoteConfigError.retryable shouldBe true
        
        val fileConfigError = InfrastructureAdapterError.ConfigurationAdapterError.LoadingError(
            source = "/etc/app/config.yaml",
            configType = ConfigurationType.FILE,
            cause = RuntimeException("File not found"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        fileConfigError.retryable shouldBe false
        
        val validationError = InfrastructureAdapterError.ConfigurationAdapterError.ValidationError(
            configKey = "database.connectionTimeout",
            expectedType = "Integer",
            actualValue = "invalid",
            validationRule = "Must be positive integer",
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        validationError.retryable shouldBe false
        
        val environmentError = InfrastructureAdapterError.ConfigurationAdapterError.EnvironmentError(
            environment = "production",
            missingKeys = listOf("DATABASE_URL", "API_KEY"),
            invalidKeys = mapOf("PORT" to "not-a-number"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        environmentError.retryable shouldBe false
    }

    "TransactionAdapterError should have correct retryability based on transaction context" {
        val sagaCoordinationError = InfrastructureAdapterError.TransactionAdapterError.CoordinationError(
            transactionId = "saga-123",
            participantCount = 3,
            failedParticipants = listOf("service-a", "service-b"),
            coordinationType = CoordinationType.SAGA,
            cause = RuntimeException("Participant timeout"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        sagaCoordinationError.retryable shouldBe true
        
        val twoPhaseCommitError = InfrastructureAdapterError.TransactionAdapterError.CoordinationError(
            transactionId = "tx-456",
            participantCount = 2,
            failedParticipants = listOf("database-primary"),
            coordinationType = CoordinationType.TWO_PHASE_COMMIT,
            cause = RuntimeException("Prepare phase failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        twoPhaseCommitError.retryable shouldBe false
        
        val partialRollbackError = InfrastructureAdapterError.TransactionAdapterError.RollbackError(
            transactionId = "tx-789",
            rollbackCause = "Business rule violation",
            partialRollback = true,
            affectedResources = listOf("scopes_table", "audit_table"),
            cause = RuntimeException("Rollback partially failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        partialRollbackError.retryable shouldBe true
        
        val completeRollbackError = InfrastructureAdapterError.TransactionAdapterError.RollbackError(
            transactionId = "tx-abc",
            rollbackCause = "System error",
            partialRollback = false,
            affectedResources = listOf("scopes_table"),
            cause = RuntimeException("Complete rollback failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        completeRollbackError.retryable shouldBe false
        
        val deadlockError = InfrastructureAdapterError.TransactionAdapterError.DeadlockError(
            transactionId = "tx-def",
            deadlockChain = listOf("tx-def", "tx-ghi", "tx-def"),
            resourcesInvolved = listOf("scope_123", "scope_456"),
            detectionTimeMs = 250,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        deadlockError.retryable shouldBe true
        
        val timeoutError = InfrastructureAdapterError.TransactionAdapterError.TimeoutError(
            transactionId = "tx-timeout",
            timeoutMs = 30000,
            elapsedMs = 30001,
            operationsCompleted = 3,
            operationsRemaining = 1,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        timeoutError.retryable shouldBe false
    }

    "All infrastructure errors should have timestamp and optional correlation ID" {
        val error = InfrastructureAdapterError.DatabaseAdapterError.ConnectionError(
            connectionString = "jdbc:postgresql://localhost:5432/test",
            poolSize = 10,
            activeConnections = 10,
            cause = RuntimeException("Connection failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        error.timestamp shouldBe testTimestamp
        error.correlationId shouldBe testCorrelationId
        error.shouldBeInstanceOf<InfrastructureAdapterError>()
        error.shouldBeInstanceOf<InfrastructureAdapterError.DatabaseAdapterError>()
    }

    "Error hierarchies should be properly sealed and exhaustive" {
        val dbError: InfrastructureAdapterError.DatabaseAdapterError = InfrastructureAdapterError.DatabaseAdapterError.ConnectionError(
            connectionString = "jdbc:test",
            poolSize = null,
            activeConnections = null,
            cause = RuntimeException("Test"),
            timestamp = testTimestamp
        )
        
        val apiError: InfrastructureAdapterError.ExternalApiAdapterError = InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
            endpoint = "http://test.com",
            method = "GET",
            networkType = NetworkErrorType.DNS_RESOLUTION,
            cause = RuntimeException("Test"),
            timestamp = testTimestamp
        )
        
        val fsError: InfrastructureAdapterError.FileSystemAdapterError = InfrastructureAdapterError.FileSystemAdapterError.IOError(
            path = "/test",
            operation = FileOperation.READ,
            cause = RuntimeException("Test"),
            timestamp = testTimestamp
        )
        
        val msgError: InfrastructureAdapterError.MessagingAdapterError = InfrastructureAdapterError.MessagingAdapterError.BrokerConnectionError(
            brokerUrl = "amqp://test",
            connectionType = ConnectionType.PRODUCER,
            cause = RuntimeException("Test"),
            timestamp = testTimestamp
        )
        
        val configError: InfrastructureAdapterError.ConfigurationAdapterError = InfrastructureAdapterError.ConfigurationAdapterError.LoadingError(
            source = "test.conf",
            configType = ConfigurationType.FILE,
            cause = RuntimeException("Test"),
            timestamp = testTimestamp
        )
        
        val txError: InfrastructureAdapterError.TransactionAdapterError = InfrastructureAdapterError.TransactionAdapterError.DeadlockError(
            transactionId = "tx-test",
            deadlockChain = listOf("tx-1", "tx-2"),
            resourcesInvolved = listOf("resource-1"),
            detectionTimeMs = 100,
            timestamp = testTimestamp
        )
        
        // All should be instances of the base type
        dbError.shouldBeInstanceOf<InfrastructureAdapterError>()
        apiError.shouldBeInstanceOf<InfrastructureAdapterError>()
        fsError.shouldBeInstanceOf<InfrastructureAdapterError>()
        msgError.shouldBeInstanceOf<InfrastructureAdapterError>()
        configError.shouldBeInstanceOf<InfrastructureAdapterError>()
        txError.shouldBeInstanceOf<InfrastructureAdapterError>()
        
        // Each should have distinct identity
        dbError shouldNotBe apiError
        apiError shouldNotBe fsError
        fsError shouldNotBe msgError
        msgError shouldNotBe configError
        configError shouldNotBe txError
    }
})