package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.CreateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock

class CreateContextViewUseCaseTest :
    DescribeSpec({
        describe("CreateContextViewHandler") {
            val contextViewRepository = mockk<ContextViewRepository>()
            val transactionManager = mockk<TransactionManager>()
            val handler = CreateContextViewHandler(contextViewRepository, transactionManager)

            beforeEach {
                // Clear all mocks before each test
                io.mockk.clearAllMocks()

                // Setup transaction manager to execute the block directly
                coEvery { transactionManager.inTransaction<ScopesError, ContextViewDto>(any()) } coAnswers {
                    val block = arg<suspend () -> Either<ScopesError, ContextViewDto>>(0)
                    block()
                }
            }

            describe("execute") {
                it("should create a context view successfully") {
                    // Given
                    val command = CreateContextViewCommand(
                        key = "client-work",
                        name = "Client Work",
                        filter = "project=acme AND priority=high",
                        description = "Context for client work",
                    )

                    val now = Clock.System.now()
                    val contextView = ContextView(
                        id = ContextViewId.generate(),
                        key = ContextViewKey.create("client-work").getOrNull()!!,
                        name = ContextViewName.create("Client Work").getOrNull()!!,
                        filter = ContextViewFilter.create("project=acme AND priority=high").getOrNull()!!,
                        description = ContextViewDescription.create("Context for client work").getOrNull()!!,
                        createdAt = now,
                        updatedAt = now,
                    )

                    coEvery { contextViewRepository.save(any()) } returns contextView.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull()?.let { dto ->
                        dto.key shouldBe "client-work"
                        dto.name shouldBe "Client Work"
                        dto.filter shouldBe "project=acme AND priority=high"
                        dto.description shouldBe "Context for client work"
                    }

                    coVerify(exactly = 1) { contextViewRepository.save(any()) }
                }

                it("should create a context view without description") {
                    // Given
                    val command = CreateContextViewCommand(
                        key = "personal",
                        name = "Personal Projects",
                        filter = "type=personal",
                        description = null,
                    )

                    val now = Clock.System.now()
                    val contextView = ContextView(
                        id = ContextViewId.generate(),
                        key = ContextViewKey.create("personal").getOrNull()!!,
                        name = ContextViewName.create("Personal Projects").getOrNull()!!,
                        filter = ContextViewFilter.create("type=personal").getOrNull()!!,
                        description = null,
                        createdAt = now,
                        updatedAt = now,
                    )

                    coEvery { contextViewRepository.save(any()) } returns contextView.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull()?.let { dto ->
                        dto.description shouldBe null
                    }
                }

                it("should return validation error for invalid key") {
                    // Given
                    val command = CreateContextViewCommand(
                        key = "",
                        name = "Invalid",
                        filter = "test=true",
                        description = null,
                    )

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    (error is ContextError.KeyInvalidFormat) shouldBe true
                }

                it("should return validation error for invalid filter syntax") {
                    // Given
                    val command = CreateContextViewCommand(
                        key = "test",
                        name = "Test",
                        filter = "((unclosed parenthesis", // This will fail balanced parentheses check
                        description = null,
                    )

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    (error is ContextError.InvalidFilter) shouldBe true
                    if (error is ContextError.InvalidFilter) {
                        error.filter shouldBe "((unclosed parenthesis"
                        error.reason shouldBe "Missing closing parenthesis at position 22"
                    }
                }

                it("should return persistence error if repository save fails") {
                    // Given
                    val command = CreateContextViewCommand(
                        key = "test",
                        name = "Test",
                        filter = "test=true",
                        description = null,
                    )

                    val errorMessage = "Database connection failed"
                    coEvery { contextViewRepository.save(any()) } returns errorMessage.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    (error is PersistenceError.StorageUnavailable) shouldBe true
                    if (error is PersistenceError.StorageUnavailable) {
                        error.operation shouldBe "save-context-view"
                        error.cause shouldBe errorMessage
                    }
                }
            }
        }
    })
