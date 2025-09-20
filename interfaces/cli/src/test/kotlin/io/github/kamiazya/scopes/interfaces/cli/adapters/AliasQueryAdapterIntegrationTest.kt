package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.getOrElse
import io.github.kamiazya.scopes.interfaces.cli.helpers.TestPortImplementations
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for AliasQueryAdapter.
 * Tests alias query operations (list aliases) using real implementations.
 */
class AliasQueryAdapterIntegrationTest :
    DescribeSpec({

        describe("AliasQueryAdapter Integration Tests") {

            lateinit var adapter: AliasQueryAdapter
            lateinit var scopeAdapter: ScopeCommandAdapter
            lateinit var aliasAdapter: AliasCommandAdapter

            beforeEach {
                val testEnvironment = TestPortImplementations.createTestEnvironment()
                adapter = AliasQueryAdapter(
                    scopeManagementQueryPort = testEnvironment.scopeManagementQueryPort,
                )
                scopeAdapter = ScopeCommandAdapter(
                    scopeManagementCommandPort = testEnvironment.scopeManagementCommandPort,
                )
                aliasAdapter = AliasCommandAdapter(
                    scopeManagementCommandPort = testEnvironment.scopeManagementCommandPort,
                )
            }

            describe("Listing Aliases") {

                context("successful alias listing") {

                    it("should list only canonical alias for new scope") {
                        runBlocking {
                            // Arrange - create a scope with auto-generated alias
                            val createResult = scopeAdapter.createScope(
                                title = "Test Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id
                            val canonicalAlias = createResult.getOrElse {
                                error("Failed to create scope")
                            }.canonicalAlias

                            // Act - list aliases
                            val result = adapter.listAliases(scopeId)

                            // Assert
                            result.shouldBeRight()
                            val aliasResult = result.getOrElse { error("Test failed to get result") }
                            aliasResult.scopeId shouldBe scopeId
                            aliasResult.totalCount shouldBe 1
                            aliasResult.aliases shouldHaveSize 1

                            val aliasInfo = aliasResult.aliases.first()
                            aliasInfo.aliasName shouldBe canonicalAlias
                            aliasInfo.isCanonical shouldBe true
                            aliasInfo.aliasType shouldNotBe null
                            aliasInfo.createdAt shouldNotBe null
                        }
                    }

                    it("should list canonical and custom aliases") {
                        runBlocking {
                            // Arrange - create scope and add custom aliases
                            val createResult = scopeAdapter.createScope(
                                title = "Multi-Alias Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id
                            val canonicalAlias = createResult.getOrElse {
                                error("Failed to create scope")
                            }.canonicalAlias

                            // Add custom aliases
                            aliasAdapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "custom-one",
                            ).getOrElse { error("Failed to add first alias") }

                            aliasAdapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "custom-two",
                            ).getOrElse { error("Failed to add second alias") }

                            aliasAdapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "custom-three",
                            ).getOrElse { error("Failed to add third alias") }

                            // Act - list all aliases
                            val result = adapter.listAliases(scopeId)

                            // Assert
                            result.shouldBeRight()
                            val aliasResult = result.getOrElse { error("Test failed to get result") }
                            aliasResult.scopeId shouldBe scopeId
                            aliasResult.totalCount shouldBe 4 // 1 canonical + 3 custom
                            aliasResult.aliases shouldHaveSize 4

                            // Check canonical alias
                            val canonicalInfo = aliasResult.aliases.find { it.isCanonical }
                            canonicalInfo shouldNotBe null
                            canonicalInfo?.aliasName shouldBe canonicalAlias

                            // Check custom aliases
                            val customAliases = aliasResult.aliases
                                .filter { !it.isCanonical }
                                .map { it.aliasName }

                            customAliases shouldContainExactlyInAnyOrder listOf(
                                "custom-one",
                                "custom-two",
                                "custom-three",
                            )
                        }
                    }

                    it("should list aliases after canonical change") {
                        runBlocking {
                            // Arrange - create scope with initial alias
                            val createResult = scopeAdapter.createScope(
                                title = "Canonical Change Test",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id
                            val oldCanonical = createResult.getOrElse {
                                error("Failed to create scope")
                            }.canonicalAlias

                            // Add custom alias and make it canonical
                            aliasAdapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "new-canonical",
                            ).getOrElse { error("Failed to add alias") }

                            aliasAdapter.setCanonicalAlias(
                                scopeId = scopeId,
                                aliasName = "new-canonical",
                            ).getOrElse { error("Failed to set canonical") }

                            // Act - list aliases
                            val result = adapter.listAliases(scopeId)

                            // Assert
                            result.shouldBeRight()
                            val aliasResult = result.getOrElse { error("Test failed to get result") }
                            aliasResult.totalCount shouldBe 2

                            // Find the new canonical
                            val newCanonicalInfo = aliasResult.aliases.find {
                                it.aliasName == "new-canonical"
                            }
                            newCanonicalInfo?.isCanonical shouldBe true

                            // Old canonical should now be regular
                            val oldCanonicalInfo = aliasResult.aliases.find {
                                it.aliasName == oldCanonical
                            }
                            oldCanonicalInfo?.isCanonical shouldBe false
                        }
                    }

                    it("should list aliases after removing some") {
                        runBlocking {
                            // Arrange - create scope with multiple aliases
                            val createResult = scopeAdapter.createScope(
                                title = "Remove Test Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id
                            val canonicalAlias = createResult.getOrElse {
                                error("Failed to create scope")
                            }.canonicalAlias

                            // Add aliases
                            aliasAdapter.addAlias(scopeId, "keep-this").getOrElse {
                                error("Failed to add keep-this")
                            }
                            aliasAdapter.addAlias(scopeId, "remove-this").getOrElse {
                                error("Failed to add remove-this")
                            }
                            aliasAdapter.addAlias(scopeId, "keep-this-too").getOrElse {
                                error("Failed to add keep-this-too")
                            }

                            // Remove one alias
                            aliasAdapter.removeAlias(scopeId, "remove-this").getOrElse {
                                error("Failed to remove alias")
                            }

                            // Act - list remaining aliases
                            val result = adapter.listAliases(scopeId)

                            // Assert
                            result.shouldBeRight()
                            val aliasResult = result.getOrElse { error("Test failed to get result") }
                            aliasResult.totalCount shouldBe 3 // canonical + 2 custom

                            val aliasNames = aliasResult.aliases.map { it.aliasName }
                            aliasNames shouldContainExactlyInAnyOrder listOf(
                                canonicalAlias,
                                "keep-this",
                                "keep-this-too",
                            )

                            // Verify removed alias is not in list
                            aliasNames.none { it == "remove-this" } shouldBe true
                        }
                    }

                    it("should handle scope created with custom alias only") {
                        runBlocking {
                            // Arrange - create scope with custom alias, no generation
                            val createResult = scopeAdapter.createScope(
                                title = "Custom Only Scope",
                                description = null,
                                parentId = null,
                                generateAlias = false,
                                customAlias = "custom-canonical",
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id

                            // Act - list aliases
                            val result = adapter.listAliases(scopeId)

                            // Assert
                            result.shouldBeRight()
                            val aliasResult = result.getOrElse { error("Test failed to get result") }
                            aliasResult.scopeId shouldBe scopeId
                            aliasResult.totalCount shouldBe 1
                            aliasResult.aliases shouldHaveSize 1

                            val aliasInfo = aliasResult.aliases.first()
                            aliasInfo.aliasName shouldBe "custom-canonical"
                            aliasInfo.isCanonical shouldBe true
                        }
                    }
                }

                context("error cases") {

                    it("should fail when scope not found") {
                        runBlocking {
                            // Act - try to list aliases for non-existent scope
                            val result = adapter.listAliases("non-existent-scope-id")

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid scope ID format") {
                        runBlocking {
                            // Act - try to list aliases with invalid ID
                            val result = adapter.listAliases("invalid-id-format")

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }

                context("edge cases") {

                    it("should handle empty alias list after all custom aliases removed") {
                        runBlocking {
                            // This test verifies that canonical alias cannot be removed
                            // So there should always be at least one alias

                            // Arrange - create scope
                            val createResult = scopeAdapter.createScope(
                                title = "Edge Case Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id
                            val canonicalAlias = createResult.getOrElse {
                                error("Failed to create scope")
                            }.canonicalAlias

                            // Add and remove custom alias
                            aliasAdapter.addAlias(scopeId, "temp-alias").getOrElse {
                                error("Failed to add alias")
                            }
                            aliasAdapter.removeAlias(scopeId, "temp-alias").getOrElse {
                                error("Failed to remove alias")
                            }

                            // Act - list aliases
                            val result = adapter.listAliases(scopeId)

                            // Assert - should still have canonical
                            result.shouldBeRight()
                            val aliasResult = result.getOrElse { error("Test failed to get result") }
                            aliasResult.totalCount shouldBe 1
                            aliasResult.aliases shouldHaveSize 1
                            aliasResult.aliases.first().isCanonical shouldBe true
                        }
                    }

                    it("should list aliases in consistent order") {
                        runBlocking {
                            // Arrange - create scope with multiple aliases
                            val createResult = scopeAdapter.createScope(
                                title = "Order Test Scope",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id
                            val canonicalAlias = createResult.getOrElse {
                                error("Failed to create scope")
                            }.canonicalAlias

                            // Add aliases in specific order
                            aliasAdapter.addAlias(scopeId, "alias-z").getOrElse {
                                error("Failed to add alias-z")
                            }
                            aliasAdapter.addAlias(scopeId, "alias-a").getOrElse {
                                error("Failed to add alias-a")
                            }
                            aliasAdapter.addAlias(scopeId, "alias-m").getOrElse {
                                error("Failed to add alias-m")
                            }

                            // Act - list aliases multiple times
                            val result1 = adapter.listAliases(scopeId)
                            val result2 = adapter.listAliases(scopeId)

                            // Assert - results should be consistent
                            result1.shouldBeRight()
                            result2.shouldBeRight()

                            val aliases1 = result1.getOrElse { error("First query failed") }.aliases
                            val aliases2 = result2.getOrElse { error("Second query failed") }.aliases

                            aliases1.map { it.aliasName } shouldBe aliases2.map { it.aliasName }
                        }
                    }
                }
            }
        }
    })
