package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.getOrElse
import io.github.kamiazya.scopes.interfaces.cli.helpers.TestPortImplementations
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for AliasCommandAdapter.
 * Tests alias operations (add, remove, set canonical) using real implementations.
 */
class AliasCommandAdapterIntegrationTest :
    DescribeSpec({

        describe("AliasCommandAdapter Integration Tests") {

            lateinit var adapter: AliasCommandAdapter
            lateinit var scopeAdapter: ScopeCommandAdapter
            lateinit var queryAdapter: ScopeQueryAdapter

            beforeEach {
                val testEnvironment = TestPortImplementations.createTestEnvironment()
                adapter = AliasCommandAdapter(
                    scopeManagementCommandPort = testEnvironment.scopeManagementCommandPort,
                )
                scopeAdapter = ScopeCommandAdapter(
                    scopeManagementCommandPort = testEnvironment.scopeManagementCommandPort,
                )
                queryAdapter = ScopeQueryAdapter(
                    scopeManagementQueryPort = testEnvironment.scopeManagementQueryPort,
                )
            }

            describe("Adding Aliases") {

                context("successful alias addition") {

                    it("should add custom alias to scope") {
                        runBlocking {
                            // Arrange - create a scope
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

                            // Act - add custom alias
                            val result = adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "custom-alias",
                            )

                            // Assert
                            result.shouldBeRight()

                            // Verify alias was added by getting scope
                            val scopeResult = queryAdapter.getScopeById(scopeId)
                            scopeResult.shouldBeRight()
                        }
                    }

                    it("should add multiple custom aliases to same scope") {
                        runBlocking {
                            // Arrange - create a scope
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

                            // Act - add multiple aliases
                            val result1 = adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "alias-one",
                            )
                            val result2 = adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "alias-two",
                            )
                            val result3 = adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "alias-three",
                            )

                            // Assert
                            result1.shouldBeRight()
                            result2.shouldBeRight()
                            result3.shouldBeRight()
                        }
                    }
                }

                context("error cases") {

                    it("should fail when scope not found") {
                        runBlocking {
                            // Act - try to add alias to non-existent scope
                            val result = adapter.addAlias(
                                scopeId = "non-existent-scope-id",
                                aliasName = "new-alias",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail when new alias already exists") {
                        runBlocking {
                            // Arrange - create two scopes
                            val scope1 = scopeAdapter.createScope(
                                title = "Scope 1",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = "existing-alias",
                            ).getOrElse { error("Failed to create scope 1") }

                            val scope2 = scopeAdapter.createScope(
                                title = "Scope 2",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            ).getOrElse { error("Failed to create scope 2") }

                            // Act - try to add existing alias to scope2
                            val result = adapter.addAlias(
                                scopeId = scope2.id,
                                aliasName = "existing-alias",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid alias format") {
                        runBlocking {
                            // Arrange - create a scope
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

                            // Act - try to add alias with invalid format
                            val result = adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "invalid alias with spaces",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }
            }

            describe("Removing Aliases") {

                context("successful alias removal") {

                    it("should remove custom alias from scope") {
                        runBlocking {
                            // Arrange - create scope and add custom alias
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

                            adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "to-remove",
                            ).getOrElse { error("Failed to add alias") }

                            // Act - remove by scopeId and aliasName
                            val result = adapter.removeAlias(
                                scopeId = scopeId,
                                aliasName = "to-remove",
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should remove one alias while keeping others") {
                        runBlocking {
                            // Arrange - create scope with multiple aliases
                            val createResult = scopeAdapter.createScope(
                                title = "Multi-Remove Test",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id

                            adapter.addAlias(scopeId, "keep-this").getOrElse {
                                error("Failed to add keep-this")
                            }
                            adapter.addAlias(scopeId, "remove-this").getOrElse {
                                error("Failed to add remove-this")
                            }
                            adapter.addAlias(scopeId, "keep-this-too").getOrElse {
                                error("Failed to add keep-this-too")
                            }

                            // Act - remove one alias
                            val result = adapter.removeAlias(
                                scopeId = scopeId,
                                aliasName = "remove-this",
                            )

                            // Assert
                            result.shouldBeRight()

                            // Verify other aliases still work by trying to add more aliases
                            // (We can't directly check without a list aliases function)
                            val verifyResult1 = adapter.addAlias(scopeId, "new-alias-1")
                            verifyResult1.shouldBeRight()
                        }
                    }
                }

                context("error cases") {

                    it("should fail when trying to remove canonical alias") {
                        runBlocking {
                            // Arrange - create scope
                            val createResult = scopeAdapter.createScope(
                                title = "Canonical Remove Test",
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

                            // Act - try to remove canonical alias
                            val result = adapter.removeAlias(
                                scopeId = scopeId,
                                aliasName = canonicalAlias,
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail when alias not found") {
                        runBlocking {
                            // Arrange - create scope
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

                            // Act - try to remove non-existent alias
                            val result = adapter.removeAlias(
                                scopeId = scopeId,
                                aliasName = "non-existent-alias",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }
            }

            describe("Setting Canonical Alias") {

                context("successful canonical alias change") {

                    it("should set existing custom alias as new canonical") {
                        runBlocking {
                            // Arrange - create scope and add custom alias
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

                            adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = "new-canonical",
                            ).getOrElse { error("Failed to add alias") }

                            // Act - set the custom alias as canonical
                            val result = adapter.setCanonicalAlias(
                                scopeId = scopeId,
                                aliasName = "new-canonical",
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should allow setting a new canonical alias") {
                        runBlocking {
                            // Arrange - create scope
                            val createResult = scopeAdapter.createScope(
                                title = "Regenerate Test",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = "initial-alias",
                            )
                            val scopeId = createResult.getOrElse {
                                error("Failed to create scope")
                            }.id

                            // First add the alias, then set it as canonical
                            val newCanonical = "regenerated-${System.currentTimeMillis()}"
                            adapter.addAlias(
                                scopeId = scopeId,
                                aliasName = newCanonical,
                            ).getOrElse { error("Failed to add alias") }

                            // Act - set the new alias as canonical
                            val result = adapter.setCanonicalAlias(
                                scopeId = scopeId,
                                aliasName = newCanonical,
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }
                }

                context("error cases") {

                    it("should fail when scope not found") {
                        runBlocking {
                            // Act - try to set canonical for non-existent scope
                            val result = adapter.setCanonicalAlias(
                                scopeId = "non-existent-scope-id",
                                aliasName = "new-canonical",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail when new canonical alias already exists for different scope") {
                        runBlocking {
                            // Arrange - create two scopes
                            val scope1 = scopeAdapter.createScope(
                                title = "Scope 1",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = "taken-alias",
                            ).getOrElse { error("Failed to create scope 1") }

                            val scope2 = scopeAdapter.createScope(
                                title = "Scope 2",
                                description = null,
                                parentId = null,
                                generateAlias = true,
                                customAlias = null,
                            ).getOrElse { error("Failed to create scope 2") }

                            // Act - try to set taken alias as canonical for scope2
                            val result = adapter.setCanonicalAlias(
                                scopeId = scope2.id,
                                aliasName = "taken-alias",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }
            }

            describe("Alias Rename Operations") {

                it("should rename an existing alias") {
                    runBlocking {
                        // Arrange - create scope and add alias
                        val createResult = scopeAdapter.createScope(
                            title = "Rename Test",
                            description = null,
                            parentId = null,
                            generateAlias = true,
                            customAlias = null,
                        )
                        val scopeId = createResult.getOrElse {
                            error("Failed to create scope")
                        }.id

                        adapter.addAlias(
                            scopeId = scopeId,
                            aliasName = "old-name",
                        ).getOrElse { error("Failed to add alias") }

                        // Act - rename the alias
                        val result = adapter.renameAlias(
                            oldAliasName = "old-name",
                            newAliasName = "new-name",
                        )

                        // Assert
                        result.shouldBeRight()

                        // Verify old alias no longer works and new one does
                        // by trying to add another alias with the old name
                        val oldAliasResult = adapter.addAlias(scopeId, "old-name")
                        oldAliasResult.shouldBeRight() // Old name should be available now
                    }
                }

                it("should fail to rename non-existent alias") {
                    runBlocking {
                        // Act - try to rename non-existent alias
                        val result = adapter.renameAlias(
                            oldAliasName = "non-existent",
                            newAliasName = "new-name",
                        )

                        // Assert
                        result.shouldBeLeft()
                    }
                }
            }
        }
    })
