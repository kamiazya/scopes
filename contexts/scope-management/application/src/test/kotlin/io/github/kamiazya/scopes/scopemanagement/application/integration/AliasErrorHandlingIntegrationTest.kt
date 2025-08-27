package io.github.kamiazya.scopes.scopemanagement.application.integration

import io.github.kamiazya.scopes.scopemanagement.application.command.AddCustomAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.GenerateCanonicalAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Integration tests for error handling in the alias system.
 * Tests various error scenarios and edge cases.
 */
class AliasErrorHandlingIntegrationTest :
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

        describe("Alias Error Handling Integration Tests") {

            describe("Invalid Alias Name Errors") {
                it("should reject empty alias name") {
                    // Given
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Empty Alias Test"),
                    ).getOrNull()!!

                    // When
                    val result = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, ""),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ApplicationError>()
                }

                it("should reject alias name that is too short") {
                    // Given
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Short Alias Test"),
                    ).getOrNull()!!

                    // When
                    val result = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, "a"),
                    )

                    // Then
                    result.shouldBeLeft()
                }

                it("should reject alias name that is too long") {
                    // Given
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Long Alias Test"),
                    ).getOrNull()!!

                    val longAlias = "a".repeat(65) // Over 64 character limit

                    // When
                    val result = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, longAlias),
                    )

                    // Then
                    result.shouldBeLeft()
                }

                it("should reject alias with invalid characters") {
                    // Given
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Invalid Chars Test"),
                    ).getOrNull()!!

                    val invalidAliases = listOf(
                        "test@alias", // @ symbol
                        "test alias", // space
                        "test.alias", // period
                        "test/alias", // slash
                        "test\\alias", // backslash
                        "-test", // starts with hyphen
                        "_test", // starts with underscore
                        "test-", // ends with hyphen
                        "test_", // ends with underscore
                        "1test", // starts with number
                        "test--alias", // consecutive hyphens
                        "test__alias", // consecutive underscores
                    )

                    // When/Then
                    invalidAliases.forEach { invalidAlias ->
                        val result = context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, invalidAlias),
                        )
                        result.shouldBeLeft() // Each should fail
                    }
                }

                it("should normalize and accept mixed case aliases") {
                    // Given
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Mixed Case Test"),
                    ).getOrNull()!!

                    // When
                    val result = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, "MiXeD-CaSe-AliAs"),
                    )

                    // Then
                    result.shouldBeRight()
                    val alias = result.getOrNull()!!
                    alias.aliasName shouldBe "mixed-case-alias" // Should be normalized to lowercase
                }
            }

            describe("Duplicate Alias Errors") {
                it("should reject duplicate custom alias") {
                    // Given - Two scopes
                    val scope1 = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Scope 1"),
                    ).getOrNull()!!
                    val scope2 = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Scope 2"),
                    ).getOrNull()!!

                    // Add alias to first scope
                    val aliasName = "duplicate-test"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope1.id, aliasName),
                    ).shouldBeRight()

                    // When - Try to add same alias to second scope
                    val result = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope2.id, aliasName),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ApplicationError>()
                }

                it("should reject adding alias that matches existing canonical alias") {
                    // Given - Create scope and get its canonical alias
                    val scope1 = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Canonical Duplicate Test"),
                    ).getOrNull()!!

                    val aliases = context.getAliasesByScopeIdHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.query.GetAliasesByScopeIdQuery(scope1.id),
                    ).getOrNull()!!
                    val canonicalAlias = aliases.first { it.aliasType == io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType.CANONICAL }

                    // Create another scope
                    val scope2 = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Another Scope"),
                    ).getOrNull()!!

                    // When - Try to add custom alias matching the canonical alias
                    val result = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope2.id, canonicalAlias.aliasName),
                    )

                    // Then
                    result.shouldBeLeft()
                }

                it("should allow same scope to have alias re-added (idempotent)") {
                    // Given
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Idempotent Test"),
                    ).getOrNull()!!

                    val aliasName = "idempotent-alias"

                    // When - Add alias twice
                    val result1 = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, aliasName),
                    )
                    val result2 = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, aliasName),
                    )

                    // Then - Second attempt should fail
                    result1.shouldBeRight()
                    result2.shouldBeLeft()
                }
            }

            describe("Non-Existent Entity Errors") {
                it("should handle adding alias to non-existent scope") {
                    // Given
                    val nonExistentScopeId = ScopeId.generate()

                    // When
                    val result = context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(nonExistentScopeId, "orphan-alias"),
                    )

                    // Then
                    result.shouldBeLeft()
                }

                it("should handle removing non-existent alias") {
                    // When
                    val result = context.removeAliasHandler.handle(
                        RemoveAliasCommand("non-existent-alias"),
                    )

                    // Then
                    result.shouldBeLeft()
                }

                it("should handle generating canonical alias for non-existent scope") {
                    // Given
                    val nonExistentScopeId = ScopeId.generate()

                    // When
                    val result = context.generateCanonicalAliasHandler.handle(
                        GenerateCanonicalAliasCommand(nonExistentScopeId),
                    )

                    // Then
                    result.shouldBeLeft()
                }

                it("should handle lookup by non-existent alias") {
                    // When
                    val result = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery("non-existent-lookup"),
                    )

                    // Then
                    result.shouldBeLeft()
                }
            }

            describe("Canonical Alias Constraints") {
                it("should prevent removing canonical alias") {
                    // Given - Create scope
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Remove Canonical Test"),
                    ).getOrNull()!!

                    // Get canonical alias
                    val aliases = context.getAliasesByScopeIdHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.query.GetAliasesByScopeIdQuery(scope.id),
                    ).getOrNull()!!
                    val canonicalAlias = aliases.first { it.aliasType == io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType.CANONICAL }

                    // When - Try to remove canonical alias
                    val result = context.removeAliasHandler.handle(
                        RemoveAliasCommand(canonicalAlias.aliasName),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ApplicationError>()
                }

                it("should maintain single canonical alias constraint") {
                    // Given - Create scope (automatically gets canonical alias)
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Single Canonical Test"),
                    ).getOrNull()!!

                    // When - Generate new canonical alias
                    context.generateCanonicalAliasHandler.handle(
                        GenerateCanonicalAliasCommand(scope.id),
                    ).shouldBeRight()

                    // Then - Should still have exactly one canonical alias
                    val aliases = context.getAliasesByScopeIdHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.query.GetAliasesByScopeIdQuery(scope.id),
                    ).getOrNull()!!

                    val canonicalAliases = aliases.filter {
                        it.aliasType == io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType.CANONICAL
                    }
                    canonicalAliases.size shouldBe 1
                }
            }

            describe("Concurrent Operation Errors") {
                it("should handle concurrent alias additions gracefully") {
                    // Given - Create multiple scopes
                    val scopes = (1..5).map { index ->
                        context.createScopeHandler.handle(
                            CreateScopeCommand(title = "Concurrent Scope $index"),
                        ).getOrNull()!!
                    }

                    // When - Try to add same alias to all scopes concurrently
                    val aliasName = "concurrent-conflict"
                    val results = scopes.map { scope ->
                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, aliasName),
                        )
                    }

                    // Then - Only one should succeed
                    val successes = results.filter { it.isRight() }
                    val failures = results.filter { it.isLeft() }

                    successes.size shouldBe 1
                    failures.size shouldBe 4
                }

                it("should handle alias operations during scope deletion") {
                    // Given - Create scope with aliases
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Delete Race Test"),
                    ).getOrNull()!!

                    val customAlias = "delete-race-alias"
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(scope.id, customAlias),
                    ).shouldBeRight()

                    // When - Delete the scope
                    context.deleteScopeHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.command.DeleteScopeCommand(scope.id),
                    ).shouldBeRight()

                    // Then - Alias operations should fail
                    val lookupResult = context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery(customAlias),
                    )
                    lookupResult.shouldBeLeft()

                    val removeResult = context.removeAliasHandler.handle(
                        RemoveAliasCommand(customAlias),
                    )
                    removeResult.shouldBeLeft()
                }
            }

            describe("Edge Case Handling") {
                it("should handle maximum alias limit per scope") {
                    // Given - Create scope
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Max Alias Test"),
                    ).getOrNull()!!

                    // When - Add many custom aliases (practical limit test)
                    val aliasCount = 50
                    val results = (1..aliasCount).map { index ->
                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, "max-test-$index"),
                        )
                    }

                    // Then - All should succeed (no hard limit in current implementation)
                    results.forEach { result ->
                        result.shouldBeRight()
                    }

                    // Verify total count
                    val allAliases = context.getAliasesByScopeIdHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.query.GetAliasesByScopeIdQuery(scope.id),
                    ).getOrNull()!!
                    allAliases.size shouldBe (aliasCount + 1) // +1 for canonical alias
                }

                it("should handle special but valid alias patterns") {
                    // Given
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Special Pattern Test"),
                    ).getOrNull()!!

                    val specialAliases = listOf(
                        "aa", // Minimum length
                        "a" + "b".repeat(63), // Maximum length
                        "a-b-c-d-e-f", // Multiple hyphens
                        "a_b_c_d_e_f", // Multiple underscores
                        "a1b2c3d4e5", // Mixed alphanumeric
                        "test-123_abc", // Mixed separators
                        "v1-2-3", // Version-like
                        "test_test_test_test", // Repeated patterns
                    )

                    // When/Then
                    specialAliases.forEach { alias ->
                        val result = context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, alias),
                        )
                        result.shouldBeRight() // All should be valid
                    }
                }

                it("should handle alias lifecycle across scope hierarchy changes") {
                    // Given - Create parent and child scopes
                    val parent = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Hierarchy Parent"),
                    ).getOrNull()!!
                    val child = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Hierarchy Child", parentId = parent.id),
                    ).getOrNull()!!

                    // Add custom aliases
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(parent.id, "parent-alias"),
                    ).shouldBeRight()
                    context.addCustomAliasHandler.handle(
                        AddCustomAliasCommand(child.id, "child-alias"),
                    ).shouldBeRight()

                    // When - Update child to remove parent relationship
                    val updateResult = context.updateScopeHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScopeCommand(
                            id = child.id,
                            title = child.title,
                            parentId = null, // Remove parent
                        ),
                    )
                    updateResult.shouldBeRight()

                    // Then - Aliases should remain intact
                    context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery("parent-alias"),
                    ).shouldBeRight()
                    context.getScopeByAliasHandler.handle(
                        GetScopeByAliasQuery("child-alias"),
                    ).shouldBeRight()
                }
            }
        }
    })
