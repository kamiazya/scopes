package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.getOrElse
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.*
import io.github.kamiazya.scopes.application.port.TransactionContext
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.test.MockLogger
import io.github.kamiazya.scopes.application.usecase.command.CreateContextView
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.domain.valueobject.ContextViewName
import io.github.kamiazya.scopes.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock

class CreateContextViewHandlerTest : StringSpec({

    val contextViewRepository = mockk<ContextViewRepository>()
    val transactionManager = mockk<TransactionManager>()
    val logger = MockLogger()
    val handler = CreateContextViewHandler(contextViewRepository, transactionManager, logger)

    beforeTest {
        // Setup transaction manager to execute the block directly
        coEvery { transactionManager.inTransaction<ApplicationError, ContextViewResult>(any()) } coAnswers {
            val block = arg<suspend TransactionContext.() -> Either<ApplicationError, ContextViewResult>>(0)
            val context = mockk<TransactionContext> {
                every { markForRollback() } returns Unit
                every { isMarkedForRollback() } returns false
                every { getTransactionId() } returns "test-tx-id"
            }
            block(context)
        }
    }

    "should create a context view successfully with valid input" {
        // Given
        val command = CreateContextView(
            key = "my-context",
            name = "MyContext",
            filterExpression = "status:active",
            description = "Test context view"
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            key = ContextViewKey.create("my-context").getOrElse { throw AssertionError("Failed to create ContextViewKey") },
            name = ContextViewName.create("MyContext").getOrElse { throw AssertionError("Failed to create ContextViewName") },
            filter = ContextViewFilter.create("status:active").getOrElse { throw AssertionError("Failed to create ContextViewFilter") },
            description = ContextViewDescription.create("Test context view").getOrElse { null },
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByKey(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns expectedContextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        val contextViewResult = result.getOrNull()!!
        contextViewResult.shouldBeInstanceOf<ContextViewResult>()
        contextViewResult.key shouldBe "my-context"
        contextViewResult.name shouldBe "MyContext"
        contextViewResult.filterExpression shouldBe "status:active"
        contextViewResult.description shouldBe "Test context view"

        coVerify(exactly = 1) { contextViewRepository.save(any()) }
    }

    "should create a context view without description" {
        // Given
        val command = CreateContextView(
            key = "simple-context",
            name = "SimpleContext",
            filterExpression = "type:task",
            description = null
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            key = ContextViewKey.create("simple-context").getOrElse { throw AssertionError("Failed to create ContextViewKey") },
            name = ContextViewName.create("SimpleContext").getOrElse { throw AssertionError("Failed to create ContextViewName") },
            filter = ContextViewFilter.create("type:task").getOrElse { throw AssertionError("Failed to create ContextViewFilter") },
            description = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByKey(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns expectedContextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrNull()?.description shouldBe null
    }

    "should return error when key is empty" {
        // Given
        val command = CreateContextView(
            key = "",  // Empty key
            name = "ValidName",
            filterExpression = "status:active",
            description = "Test"
        )

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ContextError.KeyEmpty>()
    }

    "should return error when key is invalid" {
        // Given
        val command = CreateContextView(
            key = "123-invalid",  // Key starting with number
            name = "ValidName",
            filterExpression = "status:active",
            description = "Test"
        )

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ContextError.KeyInvalidFormat>()
    }

    "should return error when name is empty" {
        // Given
        val command = CreateContextView(
            key = "valid-key",
            name = "",  // Empty name
            filterExpression = "status:active",
            description = "Test"
        )

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ContextError.NameEmpty>()
    }

    "should return error when filter expression is invalid" {
        // Given
        val command = CreateContextView(
            key = "valid-key",
            name = "ValidName",
            filterExpression = "",  // Empty filter
            description = "Test"
        )

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ContextError.FilterInvalidSyntax>()
    }

    "should return error when key already exists" {
        // Given
        val command = CreateContextView(
            key = "existing-key",
            name = "NewName",
            filterExpression = "status:active",
            description = "Test"
        )

        val existingContext = ContextView(
            id = ContextViewId.generate(),
            key = ContextViewKey.create("existing-key").getOrElse { throw AssertionError() },
            name = ContextViewName.create("ExistingName").getOrElse { throw AssertionError() },
            filter = ContextViewFilter.create("status:done").getOrElse { throw AssertionError() },
            description = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        coEvery { contextViewRepository.findByKey(any()) } returns existingContext.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ContextError.KeyAlreadyExists>()
        (error as ContextError.KeyAlreadyExists).attemptedKey shouldBe "existing-key"
    }

    "should return error when repository save fails" {
        // Given
        val command = CreateContextView(
            key = "test-context",
            name = "TestContext",
            filterExpression = "status:active",
            description = "Test"
        )

        val persistenceError = DomainPersistenceError.StorageUnavailable(
            occurredAt = Clock.System.now(),
            operation = "save",
            cause = Exception("Database error")
        )
        coEvery { contextViewRepository.findByKey(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns persistenceError.left()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
    }

    "should handle very long valid names" {
        // Given
        val longName = "A".repeat(100)  // Maximum allowed length for ContextName
        val command = CreateContextView(
            key = "long-name-context",
            name = longName,
            filterExpression = "status:active",
            description = null
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            key = ContextViewKey.create("long-name-context").getOrElse { throw AssertionError("Failed to create ContextViewKey") },
            name = ContextViewName.create(longName).getOrElse { throw AssertionError("Failed to create ContextViewName") },
            filter = ContextViewFilter.create("status:active").getOrElse { throw AssertionError("Failed to create ContextViewFilter") },
            description = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByKey(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns expectedContextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrNull()?.name shouldBe longName
    }

    "should handle complex filter expressions" {
        // Given
        val complexFilter = "status:active AND (type:task OR type:bug) AND priority:high"
        val command = CreateContextView(
            key = "complex-filter",
            name = "ComplexFilter",
            filterExpression = complexFilter,
            description = "Complex filter test"
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            key = ContextViewKey.create("complex-filter").getOrElse { throw AssertionError("Failed to create ContextViewKey") },
            name = ContextViewName.create("ComplexFilter").getOrElse { throw AssertionError("Failed to create ContextViewName") },
            filter = ContextViewFilter.create(complexFilter).getOrElse { throw AssertionError("Failed to create ContextViewFilter") },
            description = ContextViewDescription.create("Complex filter test").getOrElse { null },
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByKey(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns expectedContextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrNull()?.filterExpression shouldBe complexFilter
    }

    "should rollback transaction on error" {
        // Given
        val command = CreateContextView(
            key = "rollback-test",
            name = "RollbackTest",
            filterExpression = "status:active",
            description = "Rollback test"
        )

        val mockTransactionContext = mockk<TransactionContext> {
            every { markForRollback() } returns Unit
            every { isMarkedForRollback() } returns false
            every { getTransactionId() } returns "test-tx-id"
        }

        val exception = RuntimeException("Unexpected error")
        
        coEvery { transactionManager.inTransaction<ApplicationError, ContextViewResult>(any()) } coAnswers {
            val block = arg<suspend TransactionContext.() -> Either<ApplicationError, ContextViewResult>>(0)
            // Simulate an exception during the transaction
            try {
                block(mockTransactionContext)
            } catch (e: Exception) {
                mockTransactionContext.markForRollback()
                throw e
            }
        }

        coEvery { contextViewRepository.findByKey(any()) } throws exception

        // When
        try {
            handler(command)
        } catch (e: Exception) {
            // Expected
        }

        // Then
        coVerify(exactly = 1) { mockTransactionContext.markForRollback() }
    }
})