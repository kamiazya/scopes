package io.github.kamiazya.scopes.infrastructure.repository

import arrow.core.Either
import io.github.kamiazya.scopes.domain.error.*
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.infrastructure.error.InfrastructureAdapterError
import io.github.kamiazya.scopes.infrastructure.error.InfrastructureErrorTranslator
import io.github.kamiazya.scopes.infrastructure.error.NetworkErrorType
import io.github.kamiazya.scopes.infrastructure.error.TransactionOperation
import io.github.kamiazya.scopes.infrastructure.error.TransactionIsolationLevel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

/**
 * Tests for infrastructure error translation to repository domain errors.
 * These tests demonstrate the translation layer between infrastructure-specific errors
 * and domain-level repository errors maintaining Clean Architecture boundaries.
 */
class InfrastructureErrorTranslatorTest : StringSpec({

    val testTimestamp = Clock.System.now()
    val testCorrelationId = "test-correlation-123"
    val testScopeId = ScopeId.generate()
    val translator = InfrastructureErrorTranslator()

    // Database Adapter Error Translation Tests
    "should translate DatabaseAdapterError.ConnectionError to SaveScopeError.SystemFailure" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.ConnectionError(
            connectionString = "jdbc:postgresql://localhost:5432/test",
            poolSize = 10,
            activeConnections = 10,
            cause = RuntimeException("Connection pool exhausted"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.scopeId shouldBe testScopeId
        result.failureType shouldBe "Database connection error"
        result.cause shouldBe infrastructureError.cause
        result.retryable shouldBe true
    }

    "should translate DatabaseAdapterError.QueryError to SaveScopeError.SystemFailure" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.QueryError(
            query = "INSERT INTO scopes VALUES (?)",
            parameters = mapOf("id" to testScopeId.value),
            executionTimeMs = 5000,
            cause = RuntimeException("Syntax error in SQL"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.scopeId shouldBe testScopeId
        result.failureType shouldBe "Database query error"
        result.cause shouldBe infrastructureError.cause
        result.retryable shouldBe false
    }

    "should translate DatabaseAdapterError.DataIntegrityError to SaveScopeError.DuplicateId for primary key violations" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.DataIntegrityError(
            table = "scopes",
            constraint = "pk_scopes_id",
            violatedData = mapOf("id" to testScopeId.value),
            cause = RuntimeException("Unique constraint violation"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.DuplicateId>()
        result.scopeId shouldBe testScopeId
    }

    "should translate DatabaseAdapterError.TransactionError with deadlock to retryable SaveScopeError" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.TransactionError(
            transactionId = "tx-123",
            operation = TransactionOperation.DEADLOCK,
            isolationLevel = TransactionIsolationLevel.SERIALIZABLE,
            cause = RuntimeException("Deadlock detected"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.TransactionError>()
        result.scopeId shouldBe testScopeId
        result.transactionId shouldBe "tx-123"
        result.operation shouldBe "DEADLOCK"
        result.retryable shouldBe true
    }

    // Find Scope Error Translation Tests
    "should translate DatabaseAdapterError.ConnectionError to FindScopeError.ConnectionFailure" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.ConnectionError(
            connectionString = "jdbc:postgresql://localhost:5432/test",
            poolSize = 10,
            activeConnections = 10,
            cause = RuntimeException("Connection timeout"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToFindError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<FindScopeError.ConnectionFailure>()
        result.scopeId shouldBe testScopeId
        result.cause shouldBe infrastructureError.cause
    }

    // Exists Scope Error Translation Tests  
    "should translate DatabaseAdapterError.QueryError to ExistsScopeError.PersistenceFailure" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.QueryError(
            query = "SELECT EXISTS(SELECT 1 FROM scopes WHERE id = ?)",
            parameters = mapOf("id" to testScopeId.value),
            executionTimeMs = 1000,
            cause = RuntimeException("Query execution failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToExistsError(infrastructureError)
        
        result.shouldBeInstanceOf<ExistsScopeError.PersistenceFailure>()
        result.cause shouldBe infrastructureError.cause
    }

    // Count Scope Error Translation Tests
    "should translate DatabaseAdapterError.ResourceExhaustionError to CountScopeError.ConnectionError" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.ResourceExhaustionError(
            resource = io.github.kamiazya.scopes.infrastructure.error.DatabaseResource.MEMORY,
            currentUsage = 950000000,
            maxCapacity = 1000000000,
            cause = RuntimeException("Memory limit reached"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToCountError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<CountScopeError.ConnectionError>()
        result.parentId shouldBe testScopeId
        result.retryable shouldBe true
    }

    // External API Error Translation Tests
    "should translate ExternalApiAdapterError.NetworkError to SaveScopeError.SystemFailure" {
        val infrastructureError = InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
            endpoint = "https://external-api.com/validation",
            method = "POST",
            networkType = NetworkErrorType.CONNECTION_TIMEOUT,
            cause = RuntimeException("Network timeout"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.scopeId shouldBe testScopeId
        result.failureType shouldBe "External API network error"
        result.retryable shouldBe true
    }

    "should translate ExternalApiAdapterError.HttpError with 4xx status to non-retryable error" {
        val infrastructureError = InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
            endpoint = "https://external-api.com/validation",
            method = "POST",
            statusCode = 400,
            responseBody = "Invalid request format",
            headers = mapOf("Content-Type" to "application/json"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.retryable shouldBe false
        result.failureType shouldBe "External API HTTP error (400)"
    }

    "should translate ExternalApiAdapterError.HttpError with 5xx status to retryable error" {
        val infrastructureError = InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
            endpoint = "https://external-api.com/validation",
            method = "POST",
            statusCode = 503,
            responseBody = "Service temporarily unavailable",
            headers = mapOf("Retry-After" to "60"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.retryable shouldBe true
        result.failureType shouldBe "External API HTTP error (503)"
    }

    // FileSystem Error Translation Tests
    "should translate FileSystemAdapterError.PermissionError to non-retryable error" {
        val infrastructureError = InfrastructureAdapterError.FileSystemAdapterError.PermissionError(
            path = "/data/scopes/backup.db",
            operation = io.github.kamiazya.scopes.infrastructure.error.FileOperation.WRITE,
            requiredPermission = io.github.kamiazya.scopes.infrastructure.error.FilePermission.WRITE,
            currentPermission = io.github.kamiazya.scopes.infrastructure.error.FilePermission.READ,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.retryable shouldBe false
        result.failureType shouldBe "Filesystem permission error"
    }

    "should translate FileSystemAdapterError.StorageError to appropriate retryability" {
        val diskFullError = InfrastructureAdapterError.FileSystemAdapterError.StorageError(
            path = "/data/scopes/",
            storageType = io.github.kamiazya.scopes.infrastructure.error.StorageErrorType.DISK_FULL,
            availableSpace = 0,
            requiredSpace = 1048576,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(diskFullError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.StorageError>()
        result.scopeId shouldBe testScopeId
        result.availableSpace shouldBe 0L
        result.requiredSpace shouldBe 1048576L
        result.retryable shouldBe false
    }

    // Messaging Error Translation Tests
    "should translate MessagingAdapterError.DeliveryError to appropriate repository error" {
        val infrastructureError = InfrastructureAdapterError.MessagingAdapterError.DeliveryError(
            messageId = "msg-123",
            destination = "scope.events",
            deliveryAttempt = 3,
            maxRetries = 5,
            cause = RuntimeException("Message delivery failed"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.retryable shouldBe true
        result.failureType shouldBe "Messaging delivery error"
    }

    // Configuration Error Translation Tests
    "should translate ConfigurationAdapterError.ValidationError to non-retryable error" {
        val infrastructureError = InfrastructureAdapterError.ConfigurationAdapterError.ValidationError(
            configKey = "database.maxConnections",
            expectedType = "Integer",
            actualValue = "not-a-number",
            validationRule = "Must be positive integer",
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.retryable shouldBe false
        result.failureType shouldBe "Configuration validation error"
    }

    // Transaction Error Translation Tests
    "should translate TransactionAdapterError.DeadlockError to retryable error" {
        val infrastructureError = InfrastructureAdapterError.TransactionAdapterError.DeadlockError(
            transactionId = "tx-deadlock",
            deadlockChain = listOf("tx-1", "tx-2", "tx-1"),
            resourcesInvolved = listOf("scope_123", "scope_456"),
            detectionTimeMs = 150,
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.TransactionError>()
        result.retryable shouldBe true
        result.transactionId shouldBe "tx-deadlock"
        result.operation shouldBe "DEADLOCK"
    }

    "should preserve correlation ID and retryable characteristics across error translations" {
        val infrastructureError = InfrastructureAdapterError.DatabaseAdapterError.ConnectionError(
            connectionString = "jdbc:test",
            poolSize = null,
            activeConnections = null,
            cause = RuntimeException("Test error"),
            timestamp = testTimestamp,
            correlationId = testCorrelationId
        )
        
        val result = translator.translateToSaveError(infrastructureError, testScopeId)
        
        result.shouldBeInstanceOf<SaveScopeError.SystemFailure>()
        result.correlationId shouldBe testCorrelationId
        result.retryable shouldBe infrastructureError.retryable
    }
})