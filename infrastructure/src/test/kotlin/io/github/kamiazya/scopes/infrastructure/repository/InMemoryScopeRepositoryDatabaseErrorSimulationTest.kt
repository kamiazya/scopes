package io.github.kamiazya.scopes.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.CountScopeError
import io.github.kamiazya.scopes.domain.error.ExistsScopeError
import io.github.kamiazya.scopes.domain.error.FindScopeError
import io.github.kamiazya.scopes.domain.error.SaveScopeError
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.infrastructure.error.DatabaseAdapterError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for InMemoryScopeRepository database error simulation capabilities.
 */
class InMemoryScopeRepositoryDatabaseErrorSimulationTest : StringSpec({

    // Helper function to create test scope with valid value objects
    fun createTestScope(
        id: ScopeId = ScopeId.generate(),
        title: String = "Test Scope",
        description: String? = "Test Description",
        parentId: ScopeId? = null
    ): Scope {
        val scopeTitle = ScopeTitle.create(title).getOrNull()
            ?: error("Invalid title for test scope: '$title'")

        return Scope(
            id = id,
            title = scopeTitle,
            description = ScopeDescription.create(description).getOrNull(),
            parentId = parentId,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }

    "should simulate connection pool exhaustion error during save operation" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateConnectionPoolExhaustion(true)

            val scope = createTestScope()

            val saveResult = repository.save(scope)

            val error = saveResult.shouldBeLeft()
            error.shouldBeInstanceOf<SaveScopeError.PersistenceError>()

            // Verify the error contains connection pool details
            val dbError = error as SaveScopeError.PersistenceError
            dbError.message shouldBe "Connection pool exhausted"
            dbError.cause.shouldBeInstanceOf<RuntimeException>()
            dbError.retryable shouldBe true
            dbError.category shouldBe SaveScopeError.PersistenceError.ErrorCategory.CONNECTION
        }
    }

    "should simulate query timeout during existsById operation" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateQueryTimeout(true, timeoutMs = 5)

            val scopeId = ScopeId.generate()

            val existsResult = repository.existsById(scopeId)

            val error = existsResult.shouldBeLeft()
            error.shouldBeInstanceOf<ExistsScopeError.QueryTimeout>()

            val timeoutError = error as ExistsScopeError.QueryTimeout
            timeoutError.timeout shouldBe 5.seconds
            timeoutError.context.shouldBeInstanceOf<ExistsScopeError.ExistenceContext.ById>()
            val byIdContext = timeoutError.context as ExistsScopeError.ExistenceContext.ById
            byIdContext.scopeId shouldBe scopeId
        }
    }

    "should simulate transaction deadlock during save operation" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateTransactionDeadlock(true)

            val scope = createTestScope()

            val saveResult = repository.save(scope)

            val error = saveResult.shouldBeLeft()
            error.shouldBeInstanceOf<SaveScopeError.TransactionError>()

            val txError = error as SaveScopeError.TransactionError
            txError.transactionId shouldNotBe null
            txError.operation shouldBe "DEADLOCK_DETECTED"
            txError.retryable shouldBe true
        }
    }

    "should simulate data integrity constraint violation during save" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateDataIntegrityViolation(true, constraintName = "uk_scope_parent_title")

            val scope = createTestScope()

            val saveResult = repository.save(scope)

            val error = saveResult.shouldBeLeft()
            error.shouldBeInstanceOf<SaveScopeError.DataIntegrity>()

            val integrityError = error as SaveScopeError.DataIntegrity
            integrityError.constraint shouldBe "uk_scope_parent_title"
            integrityError.retryable shouldBe false
        }
    }

    "should simulate storage exhaustion during save operation" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateStorageExhaustion(true, availableSpace = 0, requiredSpace = 1024)

            val scope = createTestScope()

            val saveResult = repository.save(scope)

            val error = saveResult.shouldBeLeft()
            error.shouldBeInstanceOf<SaveScopeError.StorageError>()

            val storageError = error as SaveScopeError.StorageError
            storageError.availableSpace shouldBe 0
            storageError.requiredSpace shouldBe 1024
            storageError.retryable shouldBe false
        }
    }

    "should simulate connection interruption during countByParentId operation" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateConnectionInterruption(true)

            val parentId = ScopeId.generate()

            val countResult = repository.countByParentId(parentId)

            val error = countResult.shouldBeLeft()
            error.shouldBeInstanceOf<CountScopeError.ConnectionError>()

            val connectionError = error as CountScopeError.ConnectionError
            connectionError.retryable shouldBe true
            connectionError.parentId shouldBe parentId
        }
    }

    "should simulate table lock timeout during existsByParentIdAndTitle operation" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateTableLockTimeout(true, lockTimeoutMs = 3)

            val parentId = ScopeId.generate()
            val title = "Locked Title"

            val existsResult = repository.existsByParentIdAndTitle(parentId, title)

            val error = existsResult.shouldBeLeft()
            error.shouldBeInstanceOf<ExistsScopeError.LockTimeout>()

            val lockError = error as ExistsScopeError.LockTimeout
            lockError.timeout shouldBe 3.seconds
            lockError.operation shouldBe "SELECT_BY_PARENT_AND_TITLE"
            lockError.retryable shouldBe true
        }
    }

    "should simulate transaction isolation violation during findHierarchyDepth" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateIsolationViolation(true, violationType = "PHANTOM_READ")

            val scopeId = ScopeId.generate()

            val depthResult = repository.findHierarchyDepth(scopeId)

            val error = depthResult.shouldBeLeft()
            error.shouldBeInstanceOf<FindScopeError.IsolationViolation>()

            val isolationError = error as FindScopeError.IsolationViolation
            isolationError.violationType shouldBe "PHANTOM_READ"
            isolationError.scopeId shouldBe scopeId
            isolationError.retryable shouldBe true
        }
    }

    "should simulate partial failure recovery scenario" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()

            // First attempt fails with connection error
            repository.simulateConnectionPoolExhaustion(true)
            val scope = createTestScope()
            val firstAttempt = repository.save(scope)
            firstAttempt.shouldBeLeft()

            // Second attempt succeeds after connection recovery
            repository.simulateConnectionPoolExhaustion(false)
            val secondAttempt = repository.save(scope)
            val savedScope = secondAttempt.shouldBeRight()
            savedScope shouldBe scope

            // Verify scope is actually saved
            val existsResult = repository.existsById(scope.id)
            val exists = existsResult.shouldBeRight()
            exists shouldBe true
        }
    }

    "should simulate cascading failure from database to infrastructure layer" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()
            repository.simulateInfrastructureFailure(true, failureType = "CASCADING_FAILURE")

            val scope = createTestScope()

            val saveResult = repository.save(scope)

            val error = saveResult.shouldBeLeft()
            error.shouldBeInstanceOf<SaveScopeError.SystemFailure>()

            val infraError = error as SaveScopeError.SystemFailure
            infraError.failure.shouldBeInstanceOf<SaveScopeError.SystemFailure.SystemFailureType.UnknownError>()
            val unknownError = infraError.failure as SaveScopeError.SystemFailure.SystemFailureType.UnknownError
            unknownError.description shouldBe "CASCADING_FAILURE"
            infraError.retryable shouldBe false
            infraError.correlationId shouldNotBe null
        }
    }

    "should simulate different error types based on operation context" {
        runTest {
            val repository = InMemoryScopeRepositoryWithErrorSimulation()

            // Different errors for different operations
            repository.simulateOperationSpecificErrors(mapOf(
                "save" to "CONNECTION_POOL_EXHAUSTED",
                "exists" to "QUERY_TIMEOUT",
                "count" to "TABLE_LOCKED",
                "findDepth" to "TRANSACTION_ROLLBACK"
            ))

            val scope = createTestScope()
            val scopeId = ScopeId.generate()
            val parentId = ScopeId.generate()

            // Verify save operation fails with PersistenceError for connection pool exhaustion
            val saveResult = repository.save(scope)
            val saveError = saveResult.shouldBeLeft()
            saveError.shouldBeInstanceOf<SaveScopeError.PersistenceError>()
            val persistenceError = saveError as SaveScopeError.PersistenceError
            persistenceError.scopeId shouldBe scope.id
            persistenceError.message shouldBe "Connection pool exhausted"
            persistenceError.retryable shouldBe true
            persistenceError.cause?.message shouldBe "Connection pool exhausted"
            persistenceError.category shouldBe SaveScopeError.PersistenceError.ErrorCategory.CONNECTION

            // Verify exists operation fails with QueryTimeout
            val existsResult = repository.existsById(scopeId)
            val existsError = existsResult.shouldBeLeft()
            existsError.shouldBeInstanceOf<ExistsScopeError.QueryTimeout>()
            val queryTimeout = existsError as ExistsScopeError.QueryTimeout
            queryTimeout.context.shouldBeInstanceOf<ExistsScopeError.ExistenceContext.ById>()
            val byIdContext = queryTimeout.context as ExistsScopeError.ExistenceContext.ById
            byIdContext.scopeId shouldBe scopeId
            queryTimeout.timeout shouldBe 5.seconds
            queryTimeout.operation shouldBe "EXISTS_CHECK"

            // Verify count operation fails with ConnectionError for table lock
            val countResult = repository.countByParentId(parentId)
            val countError = countResult.shouldBeLeft()
            countError.shouldBeInstanceOf<CountScopeError.ConnectionError>()
            val connectionError = countError as CountScopeError.ConnectionError
            connectionError.parentId shouldBe parentId
            connectionError.retryable shouldBe true
            connectionError.cause?.message shouldBe "Table locked"

            // Verify findDepth operation fails with IsolationViolation for transaction rollback
            val depthResult = repository.findHierarchyDepth(scopeId)
            val depthError = depthResult.shouldBeLeft()
            depthError.shouldBeInstanceOf<FindScopeError.IsolationViolation>()
            val isolationViolation = depthError as FindScopeError.IsolationViolation
            isolationViolation.scopeId shouldBe scopeId
            isolationViolation.violationType shouldBe "ROLLBACK"
            isolationViolation.retryable shouldBe true
        }
    }
})

/**
 * Enhanced InMemoryScopeRepository with database error simulation capabilities.
 * This implementation demonstrates infrastructure adapter error handling patterns
 * by simulating realistic database failure scenarios.
 */
class InMemoryScopeRepositoryWithErrorSimulation : InMemoryScopeRepository() {

    private var simulateConnectionPoolExhaustion = false
    private var simulateQueryTimeout = false
    private var queryTimeoutMs = 0L
    private var simulateTransactionDeadlock = false
    private var simulateDataIntegrityViolation = false
    private var dataIntegrityConstraint = ""
    private var simulateStorageExhaustion = false
    private var availableSpace = 0L
    private var requiredSpace = 0L
    private var simulateConnectionInterruption = false
    private var simulateTableLockTimeout = false
    private var lockTimeoutMs = 0L
    private var simulateIsolationViolation = false
    private var isolationViolationType = ""
    private var simulateInfrastructureFailure = false
    private var infrastructureFailureType = ""
    private var operationSpecificErrors = mapOf<String, String>()

    fun simulateConnectionPoolExhaustion(enabled: Boolean) {
        simulateConnectionPoolExhaustion = enabled
    }

    fun simulateQueryTimeout(enabled: Boolean, timeoutMs: Long = 5000) {
        simulateQueryTimeout = enabled
        queryTimeoutMs = timeoutMs
    }

    fun simulateTransactionDeadlock(enabled: Boolean) {
        simulateTransactionDeadlock = enabled
    }

    fun simulateDataIntegrityViolation(enabled: Boolean, constraintName: String = "") {
        simulateDataIntegrityViolation = enabled
        dataIntegrityConstraint = constraintName
    }

    fun simulateStorageExhaustion(enabled: Boolean, availableSpace: Long = 0, requiredSpace: Long = 0) {
        simulateStorageExhaustion = enabled
        this.availableSpace = availableSpace
        this.requiredSpace = requiredSpace
    }

    fun simulateConnectionInterruption(enabled: Boolean) {
        simulateConnectionInterruption = enabled
    }

    fun simulateTableLockTimeout(enabled: Boolean, lockTimeoutMs: Long = 3000) {
        simulateTableLockTimeout = enabled
        this.lockTimeoutMs = lockTimeoutMs
    }

    fun simulateIsolationViolation(enabled: Boolean, violationType: String = "") {
        simulateIsolationViolation = enabled
        isolationViolationType = violationType
    }

    fun simulateInfrastructureFailure(enabled: Boolean, failureType: String = "") {
        simulateInfrastructureFailure = enabled
        infrastructureFailureType = failureType
    }

    fun simulateOperationSpecificErrors(errors: Map<String, String>) {
        operationSpecificErrors = errors
    }

    override suspend fun save(scope: Scope): Either<SaveScopeError, Scope> = either {
        if (simulateConnectionPoolExhaustion) {
            val infraError = DatabaseAdapterError.ConnectionError(
                connectionString = "jdbc:postgresql://localhost:5432/scopes",
                poolSize = 10,
                activeConnections = 10,
                causeClass = RuntimeException::class,
                causeMessage = "Connection pool exhausted",
                timestamp = Clock.System.now(),
                correlationId = "test-correlation"
            )
            raise(SaveScopeError.PersistenceError(
                scopeId = scope.id,
                message = "Connection pool exhausted",
                cause = RuntimeException("Connection pool exhausted"),
                retryable = true,
                category = SaveScopeError.PersistenceError.ErrorCategory.CONNECTION
            ))
        }

        if (simulateTransactionDeadlock) {
            raise(SaveScopeError.TransactionError(
                scopeId = scope.id,
                transactionId = "tx-${scope.id.value}",
                operation = "DEADLOCK_DETECTED",
                retryable = true,
                cause = RuntimeException("Transaction deadlock detected")
            ))
        }

        if (simulateDataIntegrityViolation) {
            raise(SaveScopeError.DataIntegrity(
                scopeId = scope.id,
                constraint = dataIntegrityConstraint,
                retryable = false,
                cause = RuntimeException("Constraint violation: $dataIntegrityConstraint")
            ))
        }

        if (simulateStorageExhaustion) {
            raise(SaveScopeError.StorageError(
                scopeId = scope.id,
                availableSpace = availableSpace,
                requiredSpace = requiredSpace,
                retryable = false,
                cause = RuntimeException("Insufficient storage space")
            ))
        }

        if (simulateInfrastructureFailure) {
            raise(SaveScopeError.SystemFailure(
                scopeId = scope.id,
                failure = SaveScopeError.SystemFailure.SystemFailureType.UnknownError(
                    description = infrastructureFailureType
                ),
                retryable = false,
                correlationId = "infra-correlation-${scope.id.value}",
                cause = RuntimeException("Infrastructure failure: $infrastructureFailureType")
            ))
        }

        if (operationSpecificErrors.containsKey("save")) {
            val errorType = operationSpecificErrors["save"]!!
            when (errorType) {
                "CONNECTION_POOL_EXHAUSTED" -> {
                    val infraError = DatabaseAdapterError.ConnectionError(
                        connectionString = "jdbc:postgresql://localhost:5432/scopes",
                        poolSize = 10,
                        activeConnections = 10,
                        causeClass = RuntimeException::class,
                        causeMessage = "Connection pool exhausted",
                        timestamp = Clock.System.now()
                    )
                    raise(SaveScopeError.PersistenceError(
                        scopeId = scope.id,
                        message = "Connection pool exhausted",
                        cause = RuntimeException("Connection pool exhausted"),
                        retryable = true,
                        category = SaveScopeError.PersistenceError.ErrorCategory.CONNECTION
                    ))
                }
            }
        }

        // If no errors simulated, use parent implementation
        mutex.withLock {
            scopes[scope.id] = scope
            scope
        }
    }

    override suspend fun existsById(id: ScopeId): Either<ExistsScopeError, Boolean> = either {
        if (simulateQueryTimeout) {
            raise(ExistsScopeError.QueryTimeout(
                context = ExistsScopeError.ExistenceContext.ById(scopeId = id),
                timeout = queryTimeoutMs.seconds,
                operation = "EXISTS_CHECK"
            ))
        }

        if (operationSpecificErrors.containsKey("exists")) {
            val errorType = operationSpecificErrors["exists"]!!
            when (errorType) {
                "QUERY_TIMEOUT" -> {
                    raise(ExistsScopeError.QueryTimeout(
                        context = ExistsScopeError.ExistenceContext.ById(scopeId = id),
                        timeout = 5.seconds,
                        operation = "EXISTS_CHECK"
                    ))
                }
            }
        }

        mutex.withLock {
            scopes.containsKey(id)
        }
    }

    override suspend fun existsByParentIdAndTitle(
        parentId: ScopeId?,
        title: String
    ): Either<ExistsScopeError, Boolean> = either {
        if (simulateTableLockTimeout) {
            raise(ExistsScopeError.LockTimeout(
                timeout = lockTimeoutMs.seconds,
                operation = "SELECT_BY_PARENT_AND_TITLE",
                retryable = true
            ))
        }

        mutex.withLock {
            val normalizedInputTitle = io.github.kamiazya.scopes.domain.util.TitleNormalizer.normalize(title)
            scopes.values.any { scope ->
                val normalizedStoredTitle = io.github.kamiazya.scopes.domain.util.TitleNormalizer.normalize(scope.title.value)
                scope.parentId == parentId && normalizedStoredTitle == normalizedInputTitle
            }
        }
    }

    override suspend fun countByParentId(parentId: ScopeId): Either<CountScopeError, Int> = either {
        if (simulateConnectionInterruption) {
            raise(CountScopeError.ConnectionError(
                parentId = parentId,
                retryable = true,
                cause = RuntimeException("Connection interrupted")
            ))
        }

        if (operationSpecificErrors.containsKey("count")) {
            val errorType = operationSpecificErrors["count"]!!
            when (errorType) {
                "TABLE_LOCKED" -> {
                    raise(CountScopeError.ConnectionError(
                        parentId = parentId,
                        retryable = true,
                        cause = RuntimeException("Table locked")
                    ))
                }
            }
        }

        mutex.withLock {
            scopes.values.count { it.parentId == parentId }
        }
    }

    override suspend fun findHierarchyDepth(scopeId: ScopeId): Either<FindScopeError, Int> = either {
        if (simulateIsolationViolation) {
            raise(FindScopeError.IsolationViolation(
                scopeId = scopeId,
                violationType = isolationViolationType,
                retryable = true
            ))
        }

        if (operationSpecificErrors.containsKey("findDepth")) {
            val errorType = operationSpecificErrors["findDepth"]!!
            when (errorType) {
                "TRANSACTION_ROLLBACK" -> {
                    raise(FindScopeError.IsolationViolation(
                        scopeId = scopeId,
                        violationType = "ROLLBACK",
                        retryable = true
                    ))
                }
            }
        }

        mutex.withLock {
            val visited = mutableSetOf<ScopeId>()

            fun calculateDepth(currentId: ScopeId?, depth: Int): Either<FindScopeError, Int> {
                return when (currentId) {
                    null -> Either.Right(depth)
                    else -> {
                        // Check for circular reference
                        if (visited.contains(currentId)) {
                            val cyclePath = visited.toList() + currentId
                            return Either.Left(FindScopeError.CircularReference(scopeId, cyclePath))
                        }
                        visited.add(currentId)

                        val scope = scopes[currentId]
                        when (scope) {
                            null -> Either.Left(FindScopeError.OrphanedScope(currentId, "Parent scope not found during traversal"))
                            else -> calculateDepth(scope.parentId, depth + 1)
                        }
                    }
                }
            }

            calculateDepth(scopeId, 0).bind()
        }
    }
}
