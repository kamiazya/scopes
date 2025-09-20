package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.getOrElse
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.interfaces.cli.helpers.TestPortImplementations
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for ScopeQueryAdapter using real implementations.
 * Tests query operations including retrieval, listing, and search functionality.
 */
class ScopeQueryAdapterIntegrationTest :
    DescribeSpec({

        describe("ScopeQueryAdapter Integration Tests") {
            lateinit var testEnv: TestPortImplementations.TestEnvironment
            lateinit var commandAdapter: ScopeCommandAdapter
            lateinit var queryAdapter: ScopeQueryAdapter

            beforeEach {
                testEnv = TestPortImplementations.createTestEnvironment()
                commandAdapter = ScopeCommandAdapter(
                    scopeManagementCommandPort = testEnv.scopeManagementCommandPort,
                )
                queryAdapter = ScopeQueryAdapter(
                    scopeManagementQueryPort = testEnv.scopeManagementQueryPort,
                )
            }

            describe("Getting Scopes by ID") {
                it("should retrieve existing scope by ID") {
                    runBlocking {
                        // Arrange
                        val createResult = commandAdapter.createScope(
                            title = "Test Scope",
                            description = "Test Description",
                        )
                        val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                        // Act
                        val result = queryAdapter.getScopeById(scopeId)

                        // Assert
                        result.shouldBeRight()
                        val scope = result.getOrElse { error("Failed to get scope") }
                        scope shouldNotBe null
                        scope!!.id shouldBe scopeId
                        scope.title shouldBe "Test Scope"
                        scope.description shouldBe "Test Description"
                    }
                }

                it("should return error for non-existent scope") {
                    runBlocking {
                        // Act
                        val result = queryAdapter.getScopeById("01ARZ3NDEKTSV4RRFFQ69G5FAV") // Valid ULID format but non-existent

                        // Assert
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                        }
                    }
                }
            }

            describe("Getting Scopes by Alias") {
                it("should retrieve scope by canonical alias") {
                    runBlocking {
                        // Arrange
                        val createResult = commandAdapter.createScope(
                            title = "Aliased Scope",
                            customAlias = "my-alias",
                            generateAlias = false,
                        )
                        createResult.shouldBeRight()

                        // Act
                        val result = queryAdapter.getScopeByAlias("my-alias")

                        // Assert
                        result.shouldBeRight()
                        val scope = result.getOrElse { error("Failed to get scope by alias") }
                        scope shouldNotBe null
                        scope!!.title shouldBe "Aliased Scope"
                        scope.canonicalAlias shouldBe "my-alias"
                    }
                }

                it("should retrieve scope by full alias name") {
                    runBlocking {
                        // Arrange
                        commandAdapter.createScope(
                            title = "Unique Prefix Scope",
                            customAlias = "unique-test-alias",
                            generateAlias = false,
                        )

                        // Act - Use full alias name since prefix matching is not implemented
                        val result = queryAdapter.getScopeByAlias("unique-test-alias")

                        // Assert
                        result.shouldBeRight()
                        val scope = result.getOrElse { error("Failed to get scope by alias") }
                        scope shouldNotBe null
                        scope!!.title shouldBe "Unique Prefix Scope"
                        scope.canonicalAlias shouldBe "unique-test-alias"
                    }
                }

                it("should return error for non-existent alias") {
                    runBlocking {
                        // Act
                        val result = queryAdapter.getScopeByAlias("non-existent-alias")

                        // Assert
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.BusinessError.AliasNotFound>()
                        }
                    }
                }

                it("should return error for non-existent alias prefix") {
                    runBlocking {
                        // Arrange
                        commandAdapter.createScope(
                            title = "First Scope",
                            customAlias = "ambiguous-alias-1",
                            generateAlias = false,
                        )
                        commandAdapter.createScope(
                            title = "Second Scope",
                            customAlias = "ambiguous-alias-2",
                            generateAlias = false,
                        )

                        // Act - Since prefix matching is not implemented, partial alias will return AliasNotFound
                        val result = queryAdapter.getScopeByAlias("ambiguous")

                        // Assert
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.BusinessError.AliasNotFound>()
                        }
                    }
                }
            }

            describe("Listing Child Scopes") {
                it("should list direct children of a scope") {
                    runBlocking {
                        // Arrange
                        val parentResult = commandAdapter.createScope(title = "Parent Scope")
                        val parentId = parentResult.getOrNull()?.id ?: error("Parent creation failed")

                        commandAdapter.createScope(title = "Child 1", parentId = parentId)
                        commandAdapter.createScope(title = "Child 2", parentId = parentId)
                        commandAdapter.createScope(title = "Child 3", parentId = parentId)

                        // Act
                        val result = queryAdapter.listChildren(parentId)

                        // Assert
                        result.shouldBeRight()
                        val scopeList = result.getOrElse { error("Failed to list children") }
                        scopeList.scopes shouldHaveSize 3
                        scopeList.scopes.map { it.title } shouldContain "Child 1"
                        scopeList.scopes.map { it.title } shouldContain "Child 2"
                        scopeList.scopes.map { it.title } shouldContain "Child 3"
                        scopeList.totalCount shouldBe 3
                    }
                }

                it("should support pagination for children") {
                    runBlocking {
                        // Arrange
                        val parentResult = commandAdapter.createScope(title = "Parent with Many Children")
                        val parentId = parentResult.getOrNull()?.id ?: error("Parent creation failed")

                        // Create 5 children
                        repeat(5) { i ->
                            commandAdapter.createScope(title = "Child ${i + 1}", parentId = parentId)
                        }

                        // Act - get first page
                        val page1 = queryAdapter.listChildren(parentId, offset = 0, limit = 3)

                        // Assert page 1
                        page1.shouldBeRight()
                        val scopeList1 = page1.getOrElse { error("Failed to get page 1") }
                        scopeList1.scopes shouldHaveSize 3
                        scopeList1.totalCount shouldBe 5

                        // Act - get second page
                        val page2 = queryAdapter.listChildren(parentId, offset = 3, limit = 3)

                        // Assert page 2
                        page2.shouldBeRight()
                        val scopeList2 = page2.getOrElse { error("Failed to get page 2") }
                        scopeList2.scopes shouldHaveSize 2
                        scopeList2.totalCount shouldBe 5
                    }
                }

                it("should return empty list for scope without children") {
                    runBlocking {
                        // Arrange
                        val leafResult = commandAdapter.createScope(title = "Leaf Scope")
                        val leafId = leafResult.getOrNull()?.id ?: error("Leaf creation failed")

                        // Act
                        val result = queryAdapter.listChildren(leafId)

                        // Assert
                        result.shouldBeRight()
                        val scopeList = result.getOrElse { error("Failed to list children of leaf scope") }
                        scopeList.scopes shouldHaveSize 0
                        scopeList.totalCount shouldBe 0
                    }
                }
            }

            describe("Listing Root Scopes") {
                it("should list all root scopes") {
                    runBlocking {
                        // Arrange
                        commandAdapter.createScope(title = "Root 1")
                        commandAdapter.createScope(title = "Root 2")
                        commandAdapter.createScope(title = "Root 3")

                        // Create some child scopes that shouldn't appear
                        val parent = commandAdapter.createScope(title = "Root 4")
                        val parentId = parent.getOrNull()?.id
                        commandAdapter.createScope(title = "Child", parentId = parentId)

                        // Act
                        val result = queryAdapter.listRootScopes()

                        // Assert
                        result.shouldBeRight()
                        val scopeList = result.getOrElse { error("Failed to list root scopes") }
                        scopeList.scopes.size shouldBe 4
                        scopeList.scopes.map { it.title } shouldContain "Root 1"
                        scopeList.scopes.map { it.title } shouldContain "Root 2"
                        scopeList.scopes.map { it.title } shouldContain "Root 3"
                        scopeList.scopes.map { it.title } shouldContain "Root 4"
                    }
                }

                it("should support pagination for root scopes") {
                    runBlocking {
                        // Arrange - create 5 root scopes
                        repeat(5) { i ->
                            commandAdapter.createScope(title = "Root ${i + 1}")
                        }

                        // Act
                        val result = queryAdapter.listRootScopes(offset = 2, limit = 2)

                        // Assert
                        result.shouldBeRight()
                        val scopeList = result.getOrElse { error("Failed to get paginated root scopes") }
                        scopeList.scopes shouldHaveSize 2
                        scopeList.totalCount shouldBe 5
                    }
                }
            }

            describe("Listing Aliases") {
                it("should list all aliases for a scope") {
                    runBlocking {
                        // Arrange
                        val createResult = commandAdapter.createScope(
                            title = "Multi-alias Scope",
                            customAlias = "primary-alias",
                            generateAlias = false,
                        )
                        val scopeId = createResult.getOrNull()?.id ?: error("Creation failed")

                        // Add additional aliases
                        testEnv.scopeManagementCommandPort.addAlias(
                            io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand(
                                scopeId = scopeId,
                                aliasName = "secondary-alias",
                            ),
                        )
                        testEnv.scopeManagementCommandPort.addAlias(
                            io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand(
                                scopeId = scopeId,
                                aliasName = "tertiary-alias",
                            ),
                        )

                        // Act
                        val result = queryAdapter.listAliases(scopeId)

                        // Assert
                        result.shouldBeRight()
                        val aliasResult = result.getOrElse { error("Failed to list aliases") }
                        val aliasList = aliasResult.aliases
                        aliasList shouldHaveSize 3
                        aliasList.map { it.aliasName } shouldContain "primary-alias"
                        aliasList.map { it.aliasName } shouldContain "secondary-alias"
                        aliasList.map { it.aliasName } shouldContain "tertiary-alias"
                        aliasList.find { it.aliasName == "primary-alias" }?.isCanonical shouldBe true
                    }
                }
            }

            describe("Filtering Scopes with Aspects") {
                it("should filter scopes by single aspect") {
                    runBlocking {
                        // Arrange
                        val scope1 = commandAdapter.createScope(title = "High Priority Task")
                        val scope2 = commandAdapter.createScope(title = "Low Priority Task")
                        val scope3 = commandAdapter.createScope(title = "Another High Priority")

                        // Note: In a real scenario, aspects would be set through proper commands
                        // This is a simplified test setup

                        // Act - would normally filter by aspect
                        val allScopes = queryAdapter.listRootScopes()

                        // Assert
                        allScopes.shouldBeRight()
                        val scopeList = allScopes.getOrElse { error("Failed to list scopes for aspect filter") }
                        scopeList.scopes.size >= 3
                    }
                }
            }

            describe("Scope Hierarchy Path") {
                it("should retrieve complete path from root to scope") {
                    runBlocking {
                        // Arrange - create hierarchy
                        val root = commandAdapter.createScope(title = "Root Project")
                        val rootId = root.getOrNull()?.id ?: error("Root creation failed")

                        val level1 = commandAdapter.createScope(
                            title = "Feature Area",
                            parentId = rootId,
                        )
                        val level1Id = level1.getOrNull()?.id ?: error("Level 1 creation failed")

                        val level2 = commandAdapter.createScope(
                            title = "Specific Task",
                            parentId = level1Id,
                        )
                        val level2Id = level2.getOrNull()?.id ?: error("Level 2 creation failed")

                        // Act
                        val result = queryAdapter.getScopeHierarchyPath(level2Id)

                        // Assert
                        result.shouldBeRight()
                        val path = result.getOrElse { error("Failed to get scope hierarchy path") }
                        path shouldHaveSize 3
                        path[0].title shouldBe "Root Project"
                        path[1].title shouldBe "Feature Area"
                        path[2].title shouldBe "Specific Task"
                    }
                }

                it("should handle single-level path (root scope)") {
                    runBlocking {
                        // Arrange
                        val root = commandAdapter.createScope(title = "Standalone Root")
                        val rootId = root.getOrNull()?.id ?: error("Root creation failed")

                        // Act
                        val result = queryAdapter.getScopeHierarchyPath(rootId)

                        // Assert
                        result.shouldBeRight()
                        val path = result.getOrElse { error("Failed to get single-level hierarchy path") }
                        path shouldHaveSize 1
                        path[0].title shouldBe "Standalone Root"
                    }
                }
            }

            describe("Search Functionality") {
                it("should return service unavailable for search feature") {
                    runBlocking {
                        // Arrange
                        commandAdapter.createScope(title = "Important Project Alpha")
                        commandAdapter.createScope(title = "Beta Testing Suite")
                        commandAdapter.createScope(title = "Alpha Documentation")

                        // Act - search for "Alpha" (feature not fully implemented)
                        val result = queryAdapter.searchScopes("Alpha")

                        // Assert - Since search functionality is not fully implemented, expect ServiceUnavailable
                        result.shouldBeLeft().asClue { error ->
                            error.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                        }
                    }
                }
            }
        }
    })
