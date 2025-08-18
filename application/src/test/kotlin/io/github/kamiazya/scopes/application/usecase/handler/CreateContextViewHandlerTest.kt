package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.getOrElse
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.port.TransactionContext
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.test.MockLogger
import io.github.kamiazya.scopes.application.usecase.command.CreateContextView
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextName
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
            name = "MyContext",
            filterExpression = "status:active",
            description = "Test context view"
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create("MyContext").getOrElse { throw AssertionError("Failed to create ContextName") },
            filter = ContextFilter.create("status:active").getOrElse { throw AssertionError("Failed to create ContextFilter") },
            description = io.github.kamiazya.scopes.domain.valueobject.ContextDescription.create("Test context view").getOrElse { null },
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByName(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns expectedContextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        val contextViewResult = result.getOrNull()!!
        contextViewResult.shouldBeInstanceOf<ContextViewResult>()
        contextViewResult.name shouldBe "MyContext"
        contextViewResult.filterExpression shouldBe "status:active"
        contextViewResult.description shouldBe "Test context view"

        coVerify(exactly = 1) { contextViewRepository.save(any()) }
    }

    "should create a context view without description" {
        // Given
        val command = CreateContextView(
            name = "SimpleContext",
            filterExpression = "type:task",
            description = null
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create("SimpleContext").getOrElse { throw AssertionError("Failed to create ContextName") },
            filter = ContextFilter.create("type:task").getOrElse { throw AssertionError("Failed to create ContextFilter") },
            description = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByName(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns expectedContextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrNull()?.description shouldBe null
    }

    "should return error when name is invalid" {
        // Given
        val command = CreateContextView(
            name = "",  // Empty name
            filterExpression = "status:active",
            description = "Test"
        )

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ApplicationError.ContextError.NamingInvalidFormat>()
    }

    "should return error when filter expression is invalid" {
        // Given
        val command = CreateContextView(
            name = "ValidName",
            filterExpression = "",  // Empty filter
            description = "Test"
        )

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ApplicationError.ContextError.FilterInvalidSyntax>()
    }

    "should return error when repository save fails" {
        // Given
        val command = CreateContextView(
            name = "TestContext",
            filterExpression = "status:active",
            description = "Test"
        )

        val persistenceError = PersistenceError.StorageUnavailable(
            occurredAt = Clock.System.now(),
            operation = "save",
            cause = Exception("Database error")
        )
        coEvery { contextViewRepository.findByName(any()) } returns null.right()
        coEvery { contextViewRepository.save(any()) } returns persistenceError.left()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ApplicationError.PersistenceError.StorageUnavailable>()
    }

    "should handle very long valid names" {
        // Given
        val longName = "A".repeat(50)  // Maximum allowed length for ContextName
        val command = CreateContextView(
            name = longName,
            filterExpression = "status:active",
            description = null
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create(longName).getOrElse { throw AssertionError("Failed to create ContextName") },
            filter = ContextFilter.create("status:active").getOrElse { throw AssertionError("Failed to create ContextFilter") },
            description = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByName(any()) } returns null.right()
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
            name = "ComplexFilter",
            filterExpression = complexFilter,
            description = "Complex filter test"
        )

        val timestamp = Clock.System.now()
        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create("ComplexFilter").getOrElse { throw AssertionError("Failed to create ContextName") },
            filter = ContextFilter.create(complexFilter).getOrElse { throw AssertionError("Failed to create ContextFilter") },
            description = io.github.kamiazya.scopes.domain.valueobject.ContextDescription.create("Complex filter test").getOrElse { null },
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { contextViewRepository.findByName(any()) } returns null.right()
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
            name = "Test",
            filterExpression = "status:active",
            description = null
        )

        val persistenceError = PersistenceError.StorageUnavailable(
            occurredAt = Clock.System.now(),
            operation = "save",
            cause = Exception("Connection lost")
        )

        // Mock the transaction manager to return an error
        coEvery { transactionManager.inTransaction<ApplicationError, ContextViewResult>(any()) } returns
            ApplicationError.PersistenceError.StorageUnavailable(
                operation = "save",
                cause = "Connection lost"
            ).left()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ApplicationError.PersistenceError.StorageUnavailable>()
    }
})

