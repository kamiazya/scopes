package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.getOrElse
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.interfaces.cli.helpers.TestPortImplementations
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking

/**
 * Integration test for ContextCommandAdapter using real implementations.
 * Tests context view management functionality without mocks.
 */
class ContextCommandAdapterIntegrationTestSimple :
    DescribeSpec({

        describe("ContextCommandAdapter Integration Tests") {
            lateinit var testEnv: TestPortImplementations.TestEnvironment
            lateinit var adapter: ContextCommandAdapter

            beforeEach {
                testEnv = TestPortImplementations.createTestEnvironment()
                adapter = ContextCommandAdapter(
                    contextViewCommandPort = testEnv.contextViewCommandPort,
                )
            }

            describe("Context Creation") {
                it("should create context view successfully") {
                    runBlocking {
                        // Arrange
                        val command = CreateContextViewCommand(
                            key = "test-context",
                            name = "Test Context",
                            filter = "status=active",
                            description = "A test context",
                        )

                        // Act
                        val result = adapter.createContext(command)

                        // Assert
                        result.shouldBeRight()

                        // Verify context was created
                        val getResult = testEnv.contextViewQueryPort.getContextView(
                            io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery("test-context"),
                        )
                        getResult.shouldBeRight()
                        val context = getResult.getOrElse { error("Failed to get context") }
                        context shouldNotBe null
                        context!!.key shouldBe "test-context"
                        context.name shouldBe "Test Context"
                        context.filter shouldBe "status=active"
                        context.description shouldBe "A test context"
                    }
                }

                it("should fail when creating duplicate context key") {
                    runBlocking {
                        // Arrange
                        val command = CreateContextViewCommand(
                            key = "duplicate-key",
                            name = "First Context",
                            filter = "type=task",
                            description = null,
                        )

                        // Create first context
                        adapter.createContext(command)

                        // Act - try to create duplicate
                        val duplicateCommand = CreateContextViewCommand(
                            key = "duplicate-key",
                            name = "Second Context",
                            filter = "type=bug",
                            description = null,
                        )
                        val result = adapter.createContext(duplicateCommand)

                        // Assert
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateContextKey>()
                        }
                    }
                }

                it("should handle invalid filter expression") {
                    runBlocking {
                        // Arrange
                        val command = CreateContextViewCommand(
                            key = "invalid-filter",
                            name = "Invalid Filter Context",
                            filter = "invalid filter expression ===",
                            description = null,
                        )

                        // Act
                        val result = adapter.createContext(command)

                        // Assert
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.InputError>()
                        }
                    }
                }
            }

            describe("Context Update") {
                it("should update existing context successfully") {
                    runBlocking {
                        // Arrange - create context first
                        val createCommand = CreateContextViewCommand(
                            key = "update-test",
                            name = "Original Name",
                            filter = "status=todo",
                            description = "Original description",
                        )
                        adapter.createContext(createCommand).shouldBeRight()

                        // Act - update context
                        val updateCommand = UpdateContextViewCommand(
                            key = "update-test",
                            name = "Updated Name",
                            filter = "status=done",
                            description = "Updated description",
                        )
                        val result = adapter.updateContext(updateCommand)

                        // Assert
                        result.shouldBeRight()

                        // Verify updates
                        val getResult = testEnv.contextViewQueryPort.getContextView(
                            io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery("update-test"),
                        )
                        getResult.shouldBeRight()
                        val context = getResult.getOrElse { error("Failed to get context") }
                        context shouldNotBe null
                        context!!.name shouldBe "Updated Name"
                        context.filter shouldBe "status=done"
                        context.description shouldBe "Updated description"
                    }
                }

                it("should fail when updating non-existent context") {
                    runBlocking {
                        // Act
                        val updateCommand = UpdateContextViewCommand(
                            key = "non-existent",
                            name = "New Name",
                            filter = null,
                            description = null,
                        )
                        val result = adapter.updateContext(updateCommand)

                        // Assert
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.BusinessError.ContextNotFound>()
                        }
                    }
                }
            }

            describe("Context Deletion") {
                it("should delete existing context successfully") {
                    runBlocking {
                        // Arrange - create context first
                        val createCommand = CreateContextViewCommand(
                            key = "delete-test",
                            name = "To Be Deleted",
                            filter = "type=temp",
                            description = null,
                        )
                        adapter.createContext(createCommand).shouldBeRight()

                        // Act - delete context
                        val deleteCommand = DeleteContextViewCommand(key = "delete-test")
                        val result = adapter.deleteContext(deleteCommand)

                        // Assert
                        result.shouldBeRight()

                        // Verify deletion
                        val getResult = testEnv.contextViewQueryPort.getContextView(
                            io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery("delete-test"),
                        )
                        getResult.shouldBeRight()
                        val context = getResult.getOrElse { error("Failed to get context") }
                        context shouldBe null
                    }
                }

                it("should fail when deleting non-existent context") {
                    runBlocking {
                        // Act
                        val deleteCommand = DeleteContextViewCommand(key = "non-existent")
                        val result = adapter.deleteContext(deleteCommand)

                        // Assert
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.BusinessError.ContextNotFound>()
                        }
                    }
                }
            }

            describe("Complex Context Filters") {
                it("should create context with complex filter expression") {
                    runBlocking {
                        // Arrange
                        val command = CreateContextViewCommand(
                            key = "complex-filter",
                            name = "Complex Filter Context",
                            filter = "priority=high AND (status=active OR status=pending)",
                            description = "Context with complex filter logic",
                        )

                        // Act
                        val result = adapter.createContext(command)

                        // Assert
                        result.shouldBeRight()
                    }
                }

                it("should create context with comparison operators") {
                    runBlocking {
                        // Arrange
                        val command = CreateContextViewCommand(
                            key = "comparison-filter",
                            name = "Comparison Filter Context",
                            filter = "estimate>=8 AND priority!=low",
                            description = null,
                        )

                        // Act
                        val result = adapter.createContext(command)

                        // Assert
                        result.shouldBeRight()
                    }
                }
            }
        }
    })
