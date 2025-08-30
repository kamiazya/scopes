package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.DomainError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.DuplicateTitleValidator
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyValidator
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.datetime.Clock

class ScopeCommandHandlerTest :
    DescribeSpec({

        describe("ScopeCommandHandler") {
            lateinit var scopeRepository: ScopeRepository
            lateinit var hierarchyValidator: ScopeHierarchyValidator
            lateinit var duplicateTitleValidator: DuplicateTitleValidator
            lateinit var transactionManager: TransactionManager
            lateinit var handler: ScopeCommandHandler

            beforeEach {
                scopeRepository = mockk()
                hierarchyValidator = mockk()
                duplicateTitleValidator = mockk()
                transactionManager = mockk()
                handler = ScopeCommandHandler(
                    scopeRepository,
                    hierarchyValidator,
                    duplicateTitleValidator,
                    transactionManager,
                )
            }

            describe("handleCreateScope with transaction") {
                it("should create scope successfully within transaction") {
                    val command = CreateScope(
                        title = "Test Scope",
                        description = "Test Description",
                        parentId = null,
                    )

                    val savedScope = Scope(
                        id = ScopeId.create("generated-id").getOrNull()!!,
                        title = ScopeTitle.create("Test Scope").getOrNull()!!,
                        description = ScopeDescription.create("Test Description").getOrNull(),
                        parentId = null,
                        aspects = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    // Mock transaction behavior
                    coEvery {
                        transactionManager.inTransaction(any())
                    } coAnswers {
                        val block = arg<suspend () -> Any>(0)
                        block()
                    }

                    // Mock validations
                    coEvery { hierarchyValidator.validate(null) } returns Unit.right()
                    coEvery { duplicateTitleValidator.validate(null, "Test Scope") } returns Unit.right()

                    // Mock repository
                    coEvery { scopeRepository.save(any()) } returns savedScope.right()

                    // Execute
                    val result = handler.handleCreateScope(command)

                    // Verify
                    result.isRight() shouldBe true
                    result.getOrNull()?.scope?.id shouldBe "generated-id"
                    result.getOrNull()?.scope?.title shouldBe "Test Scope"

                    // Verify transaction was used
                    coVerify(exactly = 1) { transactionManager.inTransaction(any()) }
                    coVerify(exactly = 1) { scopeRepository.save(any()) }
                }

                it("should rollback transaction on validation error") {
                    val command = CreateScope(
                        title = "Duplicate Title",
                        description = null,
                        parentId = null,
                    )

                    val validationError = DomainError.DuplicateScopeTitle(
                        scopeTitle = ScopeTitle.create("Duplicate Title").getOrNull()!!,
                        parentScopeId = null,
                        existingScopeId = ScopeId.create("existing-id").getOrNull()!!,
                    )

                    // Mock transaction to propagate the error
                    coEvery {
                        transactionManager.inTransaction(any())
                    } coAnswers {
                        val block = arg<suspend () -> Any>(0)
                        block()
                    }

                    // Mock validations
                    coEvery { hierarchyValidator.validate(null) } returns Unit.right()
                    coEvery { duplicateTitleValidator.validate(null, "Duplicate Title") } returns validationError.left()

                    // Execute
                    val result = handler.handleCreateScope(command)

                    // Verify
                    result.isLeft() shouldBe true
                    result.swap().getOrNull().shouldBeInstanceOf<DomainError.DuplicateScopeTitle>()

                    // Verify repository was never called
                    coVerify(exactly = 0) { scopeRepository.save(any()) }
                }

                it("should handle repository errors within transaction") {
                    val command = CreateScope(
                        title = "Test Scope",
                        description = null,
                        parentId = null,
                    )

                    val persistenceError = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.StorageUnavailable(
                        occurredAt = Clock.System.now(),
                        operation = "save",
                        cause = RuntimeException("Database error"),
                    )

                    // Mock transaction behavior
                    coEvery {
                        transactionManager.inTransaction(any())
                    } coAnswers {
                        val block = arg<suspend () -> Any>(0)
                        block()
                    }

                    // Mock validations
                    coEvery { hierarchyValidator.validate(null) } returns Unit.right()
                    coEvery { duplicateTitleValidator.validate(null, "Test Scope") } returns Unit.right()

                    // Mock repository error
                    coEvery { scopeRepository.save(any()) } returns persistenceError.left()

                    // Execute
                    val result = handler.handleCreateScope(command)

                    // Verify
                    result.isLeft() shouldBe true
                    result.swap().getOrNull().shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.StorageUnavailable>()
                }

                it("should handle parent scope validation within transaction") {
                    val parentId = "parent-id"
                    val command = CreateScope(
                        title = "Child Scope",
                        description = null,
                        parentId = parentId,
                    )

                    val hierarchyError = DomainError.CircularReference(
                        operation = "create scope",
                    )

                    // Mock transaction behavior
                    coEvery {
                        transactionManager.inTransaction(any())
                    } coAnswers {
                        val block = arg<suspend () -> Any>(0)
                        block()
                    }

                    // Mock hierarchy validation error
                    coEvery { hierarchyValidator.validate(parentId) } returns hierarchyError.left()

                    // Execute
                    val result = handler.handleCreateScope(command)

                    // Verify
                    result.isLeft() shouldBe true
                    result.swap().getOrNull().shouldBeInstanceOf<DomainError.CircularReference>()

                    // Verify other operations were not called
                    coVerify(exactly = 0) { duplicateTitleValidator.validate(any(), any()) }
                    coVerify(exactly = 0) { scopeRepository.save(any()) }
                }
            }

            describe("transaction context usage") {
                it("should provide access to transaction context within handler") {
                    val command = CreateScope(
                        title = "Test Scope",
                        description = null,
                        parentId = null,
                    )

                    var capturedTransactionId: String? = null

                    // Mock transaction with context
                    coEvery {
                        transactionManager.inTransaction(any())
                    } coAnswers {
                        val block = arg<suspend io.github.kamiazya.scopes.platform.application.port.TransactionContext.() -> Any>(0)
                        // Create a mock transaction context
                        val mockContext = object : io.github.kamiazya.scopes.platform.application.port.TransactionContext {
                            override fun markForRollback() {}
                            override fun isMarkedForRollback(): Boolean = false
                            override fun getTransactionId(): String = "test-transaction-id"
                        }
                        with(mockContext) { block() }
                    }

                    // Mock validations
                    coEvery { hierarchyValidator.validate(null) } returns Unit.right()
                    coEvery { duplicateTitleValidator.validate(null, any()) } returns Unit.right()

                    // Mock repository
                    coEvery { scopeRepository.save(any()) } answers {
                        val scope = arg<Scope>(0)
                        scope.right()
                    }

                    // Execute
                    handler.handleCreateScope(command)

                    // Verify transaction was used
                    coVerify(exactly = 1) { transactionManager.inTransaction(any()) }
                }
            }
        }
    })
