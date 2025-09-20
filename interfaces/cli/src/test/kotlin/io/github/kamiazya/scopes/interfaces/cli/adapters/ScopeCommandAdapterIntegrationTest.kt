package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.getOrElse
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
 * Integration tests for ScopeCommandAdapter using real implementations.
 * Tests the complete flow from adapter through ports to business logic.
 */
class ScopeCommandAdapterIntegrationTest :
    DescribeSpec({

        describe("ScopeCommandAdapter Integration Tests") {
            lateinit var testEnv: TestPortImplementations.TestEnvironment
            lateinit var adapter: ScopeCommandAdapter

            beforeEach {
                testEnv = TestPortImplementations.createTestEnvironment()
                adapter = ScopeCommandAdapter(
                    scopeManagementCommandPort = testEnv.scopeManagementCommandPort,
                )
            }

            describe("Creating Scopes") {
                context("successful scope creation") {
                    it("should create scope with auto-generated alias") {
                        runBlocking {
                            // Act
                            val result = adapter.createScope(
                                title = "Project Alpha",
                                description = "Main project",
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to create scope") }
                            scope.title shouldBe "Project Alpha"
                            scope.description shouldBe "Main project"
                            scope.parentId shouldBe null
                            scope.canonicalAlias shouldNotBe null
                            scope.id shouldNotBe null
                        }
                    }

                    it("should create scope with custom alias") {
                        runBlocking {
                            // Act
                            val result = adapter.createScope(
                                title = "Feature X",
                                description = "New feature",
                                parentId = null,
                                generateAlias = false,
                                customAlias = "feature-x",
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to create scope") }
                            scope.title shouldBe "Feature X"
                            scope.canonicalAlias shouldBe "feature-x"
                        }
                    }

                    it("should create child scope with parent reference") {
                        runBlocking {
                            // Arrange - create parent scope
                            val parentResult = adapter.createScope(
                                title = "Parent Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val parentId = parentResult.getOrNull()?.id ?: error("Parent creation failed")

                            // Act
                            val result = adapter.createScope(
                                title = "Child Scope",
                                description = "Sub-task",
                                parentId = parentId,
                                generateAlias = true,
                                customAlias = null,
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to create scope") }
                            scope.title shouldBe "Child Scope"
                            scope.parentId shouldBe parentId
                        }
                    }

                    it("should handle null description") {
                        runBlocking {
                            // Act
                            val result = adapter.createScope(
                                title = "Minimal Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to create scope") }
                            scope.title shouldBe "Minimal Scope"
                            scope.description shouldBe null
                        }
                    }
                }

                context("error cases") {
                    it("should fail with empty title") {
                        runBlocking {
                            // Act
                            val result = adapter.createScope(
                                title = "",
                                description = "Invalid scope",
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )

                            // Assert
                            result.shouldBeLeft().asClue { error ->
                                error.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                            }
                        }
                    }

                    it("should fail with title too long") {
                        runBlocking {
                            // Act
                            val result = adapter.createScope(
                                title = "A".repeat(201),
                                description = "Too long title",
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )

                            // Assert
                            result.shouldBeLeft().asClue { error ->
                                error.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                            }
                        }
                    }

                    it("should fail with non-existent parent") {
                        runBlocking {
                            // Act
                            val result = adapter.createScope(
                                title = "Orphan Scope",
                                description = "No parent",
                                parentId = "01ARZ3NDEKTSV4RRFFQ69G5FAV", // Valid ULID format but non-existent
                                generateAlias = true,
                                customAlias = null,
                            )

                            // Assert
                            result.shouldBeLeft().asClue { error ->
                                error.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                            }
                        }
                    }

                    it("should fail when custom alias already exists") {
                        runBlocking {
                            // Arrange - create scope with alias
                            adapter.createScope(
                                title = "First Scope",
                                customAlias = "duplicate-alias",
                                generateAlias = false,
                            )

                            // Act
                            val result = adapter.createScope(
                                title = "Second Scope",
                                customAlias = "duplicate-alias",
                                generateAlias = false,
                            )

                            // Assert
                            result.shouldBeLeft().asClue { error ->
                                error.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateAlias>()
                            }
                        }
                    }
                }
            }

            describe("Updating Scopes") {
                context("successful updates") {
                    it("should update title only") {
                        runBlocking {
                            // Arrange
                            val createResult = adapter.createScope(
                                title = "Original Title",
                                description = "Original Description",
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                            // Act
                            val result = adapter.updateScope(
                                id = scopeId,
                                title = "Updated Title",
                                description = null, // Keep original
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to update scope") }
                            scope.title shouldBe "Updated Title"
                            scope.description shouldBe "Original Description"
                        }
                    }

                    it("should update description only") {
                        runBlocking {
                            // Arrange
                            val createResult = adapter.createScope(
                                title = "Test Scope",
                                description = "Old Description",
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                            // Act
                            val result = adapter.updateScope(
                                id = scopeId,
                                title = null, // Keep original
                                description = "New Description",
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to update scope") }
                            scope.title shouldBe "Test Scope"
                            scope.description shouldBe "New Description"
                        }
                    }

                    it("should update both title and description") {
                        runBlocking {
                            // Arrange
                            val createResult = adapter.createScope(
                                title = "Old Title",
                                description = "Old Description",
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                            // Act
                            val result = adapter.updateScope(
                                id = scopeId,
                                title = "New Title",
                                description = "New Description",
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to update scope") }
                            scope.title shouldBe "New Title"
                            scope.description shouldBe "New Description"
                        }
                    }

                    it("should clear description when empty string provided") {
                        runBlocking {
                            // Arrange
                            val createResult = adapter.createScope(
                                title = "Test Scope",
                                description = "Has Description",
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                            // Act
                            val result = adapter.updateScope(
                                id = scopeId,
                                description = "", // Clear description
                            )

                            // Assert
                            result.shouldBeRight()
                            val scope = result.getOrElse { error("Failed to update scope") }
                            scope.description shouldBe null
                        }
                    }
                }

                context("error cases") {
                    it("should fail when scope not found") {
                        runBlocking {
                            // Act
                            val result = adapter.updateScope(
                                id = "01ARZ3NDEKTSV4RRFFQ69G5FAV", // Valid ULID format but non-existent
                                title = "New Title",
                            )

                            // Assert
                            result.shouldBeLeft().asClue { error ->
                                error.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                            }
                        }
                    }

                    it("should fail with invalid title") {
                        runBlocking {
                            // Arrange
                            val createResult = adapter.createScope(
                                title = "Test Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                            // Act
                            val result = adapter.updateScope(
                                id = scopeId,
                                title = "", // Empty title
                            )

                            // Assert
                            result.shouldBeLeft().asClue { error ->
                                error.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                            }
                        }
                    }
                }
            }

            describe("Deleting Scopes") {
                context("successful deletion") {
                    it("should delete single scope without children") {
                        runBlocking {
                            // Arrange
                            val createResult = adapter.createScope(
                                title = "To Delete",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                            // Act
                            val deleteResult = adapter.deleteScope(scopeId)

                            // Assert
                            deleteResult.shouldBeRight()

                            // Verify scope is deleted
                            val getResult = testEnv.scopeManagementQueryPort.getScope(
                                io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery(scopeId),
                            )
                            getResult.shouldBeRight()
                            val scope = getResult.getOrElse { error("Failed to get scope") }
                            scope shouldBe null
                        }
                    }

                    it("should delete scope and all its children") {
                        runBlocking {
                            // Arrange - create hierarchy
                            val parentResult = adapter.createScope(
                                title = "Parent",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val parentId = parentResult.getOrNull()?.id ?: error("Parent creation failed")

                            val child1Result = adapter.createScope(
                                title = "Child 1",
                                description = null,
                                parentId = parentId,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val child1Id = child1Result.getOrNull()?.id ?: error("Child 1 creation failed")

                            adapter.createScope(
                                title = "Grandchild",
                                description = null,
                                parentId = child1Id,
                                generateAlias = true,
                                customAlias = null,
                            )

                            // Act
                            val result = adapter.deleteScope(parentId, cascade = true)

                            // Assert
                            result.shouldBeRight()

                            // Verify all are deleted
                            val getParent = testEnv.scopeManagementQueryPort.getScope(
                                io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery(parentId),
                            )
                            getParent.shouldBeRight()
                            val scope = getParent.getOrElse { error("Failed to get parent") }
                            scope shouldBe null
                        }
                    }
                }

                context("error cases") {
                    it("should fail when scope not found") {
                        runBlocking {
                            // Act
                            val result = adapter.deleteScope("01ARZ3NDEKTSV4RRFFQ69G5FAV") // Valid ULID format but non-existent

                            // Assert
                            result.shouldBeLeft().asClue { error ->
                                error.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                            }
                        }
                    }
                }
            }

            describe("Project Initialization") {
                it("should initialize new project as root scope") {
                    runBlocking {
                        // Act
                        val result = adapter.initializeProject(
                            name = "My New Project",
                            description = "Project description",
                        )

                        // Assert
                        result.shouldBeRight()
                        val project = result.getOrElse { error("Failed to initialize project") }
                        project.title shouldBe "My New Project"
                        project.description shouldBe "Project description"
                        project.parentId shouldBe null // Root scope
                        project.canonicalAlias shouldNotBe null
                    }
                }
            }
        }
    })
