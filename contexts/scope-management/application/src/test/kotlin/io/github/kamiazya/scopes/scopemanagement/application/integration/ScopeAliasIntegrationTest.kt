package io.github.kamiazya.scopes.scopemanagement.application.integration

import io.github.kamiazya.scopes.scopemanagement.application.command.AddCustomAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.GenerateCanonicalAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.query.GetAliasesByScopeIdQuery
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery
import io.github.kamiazya.scopes.scopemanagement.application.query.SearchAliasesQuery
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

/**
 * Integration tests for the complete scope alias functionality.
 * Tests the full flow from command handlers through domain logic to repositories.
 */
class ScopeAliasIntegrationTest :
    DescribeSpec({

        lateinit var context: IntegrationTestContext

        beforeSpec {
            IntegrationTestFixture.setupTestDependencies()
        }

        afterSpec {
            IntegrationTestFixture.tearDownTestDependencies()
        }

        beforeEach {
            context = IntegrationTestFixture.createTestContext()
        }

        describe("Scope Alias Integration Tests") {

            describe("Canonical Alias Generation") {
                it("should automatically generate canonical alias when creating scope") {
                    // Given
                    val createCommand = CreateScopeCommand(
                        title = "Test Project",
                        description = "A test project for alias integration",
                    )

                    // When
                    val createResult = context.createScopeHandler.handle(createCommand)

                    // Then
                    createResult.shouldBeRight()
                    val scope = createResult.getOrNull()!!

                    // Verify canonical alias was generated
                    val aliasesQuery = GetAliasesByScopeIdQuery(scope.id)
                    val aliasesResult = context.getAliasesByScopeIdHandler.handle(aliasesQuery)

                    aliasesResult.shouldBeRight()
                    val aliases = aliasesResult.getOrNull()!!
                    aliases.shouldHaveSize(1)

                    val canonicalAlias = aliases.first()
                    canonicalAlias.aliasType shouldBe AliasType.CANONICAL
                    canonicalAlias.aliasName shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                }

                it("should handle canonical alias generation for child scopes") {
                    // Given - Create parent scope
                    val parentCommand = CreateScopeCommand(
                        title = "Parent Project",
                    )
                    val parentResult = context.createScopeHandler.handle(parentCommand)
                    val parentScope = parentResult.getOrNull()!!

                    // When - Create child scope
                    val childCommand = CreateScopeCommand(
                        title = "Child Module",
                        parentId = parentScope.id,
                    )
                    val childResult = context.createScopeHandler.handle(childCommand)

                    // Then
                    childResult.shouldBeRight()
                    val childScope = childResult.getOrNull()!!

                    // Both parent and child should have canonical aliases
                    val parentAliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(parentScope.id),
                    )
                    val childAliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(childScope.id),
                    )

                    parentAliasesResult.getOrNull()!!.shouldHaveSize(1)
                    childAliasesResult.getOrNull()!!.shouldHaveSize(1)

                    // Aliases should be different
                    val parentAlias = parentAliasesResult.getOrNull()!!.first()
                    val childAlias = childAliasesResult.getOrNull()!!.first()
                    parentAlias.aliasName shouldNotBe childAlias.aliasName
                }

                it("should regenerate canonical alias on demand") {
                    // Given - Create scope with initial canonical alias
                    val createCommand = CreateScopeCommand(title = "Regeneration Test")
                    val createResult = context.createScopeHandler.handle(createCommand)
                    val scope = createResult.getOrNull()!!

                    // Get initial canonical alias
                    val initialAliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val initialAlias = initialAliasesResult.getOrNull()!!.first()

                    // When - Generate new canonical alias
                    val regenerateCommand = GenerateCanonicalAliasCommand(scope.id)
                    val regenerateResult = context.generateCanonicalAliasHandler.handle(regenerateCommand)

                    // Then
                    regenerateResult.shouldBeRight()
                    val newAlias = regenerateResult.getOrNull()!!

                    // New alias should be different but same type
                    newAlias.aliasName shouldNotBe initialAlias.aliasName
                    newAlias.aliasType shouldBe AliasType.CANONICAL

                    // Should still have only one canonical alias
                    val finalAliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val canonicalAliases = finalAliasesResult.getOrNull()!!
                        .filter { it.aliasType == AliasType.CANONICAL }
                    canonicalAliases.shouldHaveSize(1)
                    canonicalAliases.first().aliasName shouldBe newAlias.aliasName
                }
            }

            describe("Custom Alias Management") {
                it("should add custom alias to existing scope") {
                    // Given - Create scope
                    val createCommand = CreateScopeCommand(title = "Custom Alias Test")
                    val createResult = context.createScopeHandler.handle(createCommand)
                    val scope = createResult.getOrNull()!!

                    // When - Add custom alias
                    val addAliasCommand = AddCustomAliasCommand(
                        scopeId = scope.id,
                        aliasName = "my-custom-alias",
                    )
                    val addResult = context.addCustomAliasHandler.handle(addAliasCommand)

                    // Then
                    addResult.shouldBeRight()
                    val customAlias = addResult.getOrNull()!!
                    customAlias.aliasName shouldBe "my-custom-alias"
                    customAlias.aliasType shouldBe AliasType.CUSTOM

                    // Verify scope now has both canonical and custom aliases
                    val aliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val aliases = aliasesResult.getOrNull()!!
                    aliases.shouldHaveSize(2)
                    aliases.map { it.aliasType } shouldContain AliasType.CANONICAL
                    aliases.map { it.aliasType } shouldContain AliasType.CUSTOM
                }

                it("should prevent duplicate custom aliases") {
                    // Given - Create two scopes
                    val scope1Result = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Scope 1"),
                    )
                    val scope2Result = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Scope 2"),
                    )
                    val scope1 = scope1Result.getOrNull()!!
                    val scope2 = scope2Result.getOrNull()!!

                    // Add custom alias to first scope
                    val aliasName = "unique-alias"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope1.id, aliasName),
                    ).shouldBeRight()

                    // When - Try to add same alias to second scope
                    val duplicateResult = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope2.id, aliasName),
                    )

                    // Then
                    duplicateResult.shouldBeLeft()
                }

                it("should allow multiple custom aliases per scope") {
                    // Given - Create scope
                    val createResult = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Multi-Alias Test"),
                    )
                    val scope = createResult.getOrNull()!!

                    // When - Add multiple custom aliases
                    val aliases = listOf("alias-one", "alias-two", "alias-three")
                    aliases.forEach { aliasName ->
                        val result = context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, aliasName),
                        )
                        result.shouldBeRight()
                    }

                    // Then
                    val aliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val allAliases = aliasesResult.getOrNull()!!
                    allAliases.shouldHaveSize(4) // 1 canonical + 3 custom

                    val customAliases = allAliases.filter { it.aliasType == AliasType.CUSTOM }
                    customAliases.shouldHaveSize(3)
                    customAliases.map { it.aliasName } shouldBe aliases
                }

                it("should remove custom alias") {
                    // Given - Create scope with custom alias
                    val createResult = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Remove Alias Test"),
                    )
                    val scope = createResult.getOrNull()!!

                    val aliasName = "removable-alias"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, aliasName),
                    ).shouldBeRight()

                    // When - Remove the custom alias
                    val removeResult = context.removeAliasHandler.handle(
                        RemoveAliasCommand(aliasName),
                    )

                    // Then
                    removeResult.shouldBeRight()

                    // Verify alias is removed
                    val aliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val remainingAliases = aliasesResult.getOrNull()!!
                    remainingAliases.shouldHaveSize(1) // Only canonical remains
                    remainingAliases.first().aliasType shouldBe AliasType.CANONICAL
                }

                it("should prevent removing canonical alias") {
                    // Given - Create scope
                    val createResult = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Canonical Remove Test"),
                    )
                    val scope = createResult.getOrNull()!!

                    // Get the canonical alias
                    val aliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val canonicalAlias = aliasesResult.getOrNull()!!
                        .first { it.aliasType == AliasType.CANONICAL }

                    // When - Try to remove canonical alias
                    val removeResult = context.removeAliasHandler.handle(
                        RemoveAliasCommand(canonicalAlias.aliasName),
                    )

                    // Then
                    removeResult.shouldBeLeft()
                }
            }

            describe("Scope Lookup by Alias") {
                it("should find scope by canonical alias") {
                    // Given - Create scope
                    val createResult = context.createScopeHandler.handle(
                        CreateScopeCommand(
                            title = "Lookup Test",
                            description = "Test scope for alias lookup",
                        ),
                    )
                    val scope = createResult.getOrNull()!!

                    // Get canonical alias
                    val aliasesResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val canonicalAlias = aliasesResult.getOrNull()!!.first()

                    // When - Look up scope by alias
                    val lookupResult = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(canonicalAlias.aliasName),
                    )

                    // Then
                    lookupResult.shouldBeRight()
                    val foundScope = lookupResult.getOrNull()!!
                    foundScope.id shouldBe scope.id
                    foundScope.title shouldBe scope.title
                    foundScope.description shouldBe scope.description
                }

                it("should find scope by custom alias") {
                    // Given - Create scope with custom alias
                    val createResult = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Custom Lookup Test"),
                    )
                    val scope = createResult.getOrNull()!!

                    val customAlias = "find-me-alias"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, customAlias),
                    ).shouldBeRight()

                    // When - Look up scope by custom alias
                    val lookupResult = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(customAlias),
                    )

                    // Then
                    lookupResult.shouldBeRight()
                    val foundScope = lookupResult.getOrNull()!!
                    foundScope.id shouldBe scope.id
                }

                it("should handle case-insensitive alias lookup") {
                    // Given - Create scope with custom alias
                    val createResult = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Case Test"),
                    )
                    val scope = createResult.getOrNull()!!

                    val customAlias = "case-sensitive-test"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, customAlias),
                    ).shouldBeRight()

                    // When - Look up with different cases
                    val upperResult = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery("CASE-SENSITIVE-TEST"),
                    )
                    val mixedResult = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery("Case-Sensitive-Test"),
                    )

                    // Then - Both should find the scope
                    upperResult.shouldBeRight()
                    mixedResult.shouldBeRight()
                    upperResult.getOrNull()!!.id shouldBe scope.id
                    mixedResult.getOrNull()!!.id shouldBe scope.id
                }

                it("should return error for non-existent alias") {
                    // When - Look up non-existent alias
                    val result = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery("non-existent-alias"),
                    )

                    // Then
                    result.shouldBeLeft()
                }
            }

            describe("Alias Search") {
                it("should search aliases by prefix") {
                    // Given - Create multiple scopes with custom aliases
                    val prefix = "search-test-"
                    val aliases = listOf(
                        "${prefix}alpha",
                        "${prefix}beta",
                        "${prefix}gamma",
                    )

                    aliases.forEach { aliasName ->
                        val scope = context.createScopeHandler.handle(
                            CreateScopeCommand(title = "Search Test $aliasName"),
                        ).getOrNull()!!

                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, aliasName),
                        ).shouldBeRight()
                    }

                    // When - Search by prefix
                    val searchResult = context.searchAliasesHandler.handle(
                        SearchAliasesQuery(prefix, limit = 10),
                    )

                    // Then
                    searchResult.shouldBeRight()
                    val foundAliases = searchResult.getOrNull()!!
                    foundAliases.shouldHaveSize(3)
                    foundAliases.map { it.aliasName }.sorted() shouldBe aliases.sorted()
                }

                it("should respect search limit") {
                    // Given - Create many scopes with similar aliases
                    val prefix = "limit-test-"
                    repeat(10) { index ->
                        val scope = context.createScopeHandler.handle(
                            CreateScopeCommand(title = "Limit Test $index"),
                        ).getOrNull()!!

                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, "${prefix}$index"),
                        ).shouldBeRight()
                    }

                    // When - Search with limit
                    val searchResult = context.searchAliasesHandler.handle(
                        SearchAliasesQuery(prefix, limit = 5),
                    )

                    // Then
                    searchResult.shouldBeRight()
                    val foundAliases = searchResult.getOrNull()!!
                    foundAliases.shouldHaveSize(5)
                }

                it("should return both canonical and custom aliases in search") {
                    // Given - Create scope (gets canonical alias automatically)
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Mixed Search Test"),
                    ).getOrNull()!!

                    // Get the generated canonical alias
                    val canonicalResult = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    )
                    val canonicalAlias = canonicalResult.getOrNull()!!.first()

                    // Extract prefix from canonical alias (e.g., "bold" from "bold-tiger-x7k")
                    val canonicalPrefix = canonicalAlias.aliasName.split("-").first()

                    // Add custom alias with same prefix
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, "$canonicalPrefix-custom"),
                    ).shouldBeRight()

                    // When - Search by prefix
                    val searchResult = context.searchAliasesHandler.handle(
                        SearchAliasesQuery(canonicalPrefix, limit = 10),
                    )

                    // Then
                    searchResult.shouldBeRight()
                    val foundAliases = searchResult.getOrNull()!!
                    foundAliases.size shouldBe { it >= 2 } // At least the two we know about

                    val aliasTypes = foundAliases.map { it.aliasType }.distinct()
                    aliasTypes shouldContain AliasType.CANONICAL
                    aliasTypes shouldContain AliasType.CUSTOM
                }
            }

            describe("End-to-End Workflows") {
                it("should handle complete scope lifecycle with aliases") {
                    // 1. Create parent scope
                    val parentResult = context.createScopeHandler.handle(
                        CreateScopeCommand(
                            title = "E2E Parent",
                            description = "Parent scope for E2E test",
                        ),
                    )
                    val parentScope = parentResult.getOrNull()!!

                    // 2. Add custom alias to parent
                    val parentCustomAlias = "e2e-parent"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(parentScope.id, parentCustomAlias),
                    ).shouldBeRight()

                    // 3. Create child scope
                    val childResult = context.createScopeHandler.handle(
                        CreateScopeCommand(
                            title = "E2E Child",
                            parentId = parentScope.id,
                        ),
                    )
                    val childScope = childResult.getOrNull()!!

                    // 4. Add custom alias to child
                    val childCustomAlias = "e2e-child"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(childScope.id, childCustomAlias),
                    ).shouldBeRight()

                    // 5. Verify lookups work
                    val parentByAlias = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(parentCustomAlias),
                    ).getOrNull()!!
                    parentByAlias.id shouldBe parentScope.id

                    val childByAlias = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(childCustomAlias),
                    ).getOrNull()!!
                    childByAlias.id shouldBe childScope.id

                    // 6. Search for all E2E aliases
                    val searchResult = context.searchAliasesHandler.handle(
                        SearchAliasesQuery("e2e", limit = 10),
                    ).getOrNull()!!
                    searchResult.size shouldBe { it >= 2 } // At least our two custom aliases

                    // 7. Delete child scope
                    context.deleteScopeHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.command.DeleteScopeCommand(childScope.id),
                    ).shouldBeRight()

                    // 8. Verify child alias is gone
                    val childLookupAfterDelete = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(childCustomAlias),
                    )
                    childLookupAfterDelete.shouldBeLeft()

                    // 9. Parent alias should still work
                    val parentStillExists = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(parentCustomAlias),
                    )
                    parentStillExists.shouldBeRight()
                }

                it("should maintain alias integrity across updates") {
                    // 1. Create scope
                    val createResult = context.createScopeHandler.handle(
                        CreateScopeCommand(
                            title = "Original Title",
                            description = "Original Description",
                        ),
                    )
                    val scope = createResult.getOrNull()!!

                    // 2. Get canonical alias
                    val initialAliases = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    ).getOrNull()!!
                    val canonicalAlias = initialAliases.first { it.aliasType == AliasType.CANONICAL }

                    // 3. Add custom aliases
                    val customAliases = listOf("update-test-1", "update-test-2")
                    customAliases.forEach { alias ->
                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, alias),
                        ).shouldBeRight()
                    }

                    // 4. Update scope details
                    val updateResult = context.updateScopeHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScopeCommand(
                            id = scope.id,
                            title = "Updated Title",
                            description = "Updated Description",
                        ),
                    )
                    updateResult.shouldBeRight()

                    // 5. Verify all aliases still work
                    val lookupByCanonical = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(canonicalAlias.aliasName),
                    ).getOrNull()!!
                    lookupByCanonical.title shouldBe "Updated Title"
                    lookupByCanonical.description shouldBe "Updated Description"

                    customAliases.forEach { alias ->
                        val lookupByCustom = context.getScopeByAliasHandler.handle(
                            GetScopeByAliasQuery(alias),
                        ).getOrNull()!!
                        lookupByCustom.title shouldBe "Updated Title"
                    }

                    // 6. Verify alias count unchanged
                    val finalAliases = context.getAliasesByScopeIdHandler.handle(
                        GetAliasesByScopeIdQuery(scope.id),
                    ).getOrNull()!!
                    finalAliases.shouldHaveSize(3) // 1 canonical + 2 custom
                }
            }
        }
    })
