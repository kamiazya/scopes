package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.CreateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
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
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError as DomainPersistenceError

class CreateContextViewUseCaseTest :
    DescribeSpec({
        describe("CreateContextViewHandler") {
            val contextViewRepository = mockk<ContextViewRepository>()
            val transactionManager = mockk<TransactionManager>()
            val applicationErrorMapper = mockk<ApplicationErrorMapper>()
            val logger = mockk<io.github.kamiazya.scopes.platform.observability.logging.Logger>(relaxed = true)
            val handler = CreateContextViewHandler(contextViewRepository, applicationErrorMapper, transactionManager, logger)

            beforeEach {
                // Clear all mocks before each test
                io.mockk.clearAllMocks()

                // Setup transaction manager to execute the block directly
                coEvery {
                    transactionManager.inTransaction<ScopeContractError, ContextViewDto>(any())
                } coAnswers {
                    val block = arg<
                        suspend io.github.kamiazya.scopes.platform.application.port.TransactionContext.() ->
                        Either<ScopeContractError, ContextViewDto>,
                        >(0)
                    // Create a mock transaction context
                    val transactionContext =
                        mockk<io.github.kamiazya.scopes.platform.application.port.TransactionContext>()
                    block(transactionContext)
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

                    // Mock the mapper to return appropriate contract error
                    coEvery {
                        applicationErrorMapper.mapDomainError(any<io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError>())
                    } returns ScopeContractError.InputError.InvalidContextKey(
                        key = "",
                        validationFailure = ScopeContractError.ContextKeyValidationFailure.Empty,
                    )

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    (error is ScopeContractError.InputError.InvalidContextKey) shouldBe true
                    if (error is ScopeContractError.InputError.InvalidContextKey) {
                        error.key shouldBe ""
                        error.validationFailure shouldBe ScopeContractError.ContextKeyValidationFailure.Empty
                    }
                }

                it("should return validation error for invalid filter syntax") {
                    // Given
                    val command = CreateContextViewCommand(
                        key = "test",
                        name = "Test",
                        filter = "((unclosed parenthesis", // This will fail balanced parentheses check
                        description = null,
                    )

                    // Mock the mapper to return appropriate contract error
                    coEvery {
                        applicationErrorMapper.mapDomainError(any<io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError>())
                    } returns ScopeContractError.InputError.InvalidContextFilter(
                        filter = "((unclosed parenthesis",
                        validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                            expression = "((unclosed parenthesis",
                            errorType = "UnbalancedParentheses",
                        ),
                    )

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    (error is ScopeContractError.InputError.InvalidContextFilter) shouldBe true
                    if (error is ScopeContractError.InputError.InvalidContextFilter) {
                        error.filter shouldBe "((unclosed parenthesis"
                        val failure = error.validationFailure as? ScopeContractError.ContextFilterValidationFailure.InvalidSyntax
                        failure?.errorType shouldBe "UnbalancedParentheses"
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
                    val persistenceError = DomainPersistenceError.ConcurrencyConflict(
                        entityType = "ContextView",
                        entityId = "test-id",
                        expectedVersion = "1",
                        actualVersion = "2",
                    )
                    coEvery { contextViewRepository.save(any()) } returns persistenceError.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    // Since repository errors are mapped to ServiceUnavailable in the handler
                    (error is ScopeContractError.SystemError.ServiceUnavailable) shouldBe true
                    if (error is ScopeContractError.SystemError.ServiceUnavailable) {
                        error.service shouldBe "context-view-repository"
                    }
                }
            }
        }
    })
