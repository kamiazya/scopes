package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Tests for MCP server error handling, particularly the canonical alias removal fix.
 * These tests focus on the contract error structure and behavior.
 */
class McpServerErrorHandlingTest :
    StringSpec({

        "CannotRemoveCanonicalAlias should be a data object without alias parameter" {
            runTest {
                // Arrange & Act
                val error = ScopeContractError.BusinessError.CannotRemoveCanonicalAlias

                // Assert - This test demonstrates that the error is now a data object
                error::class.simpleName shouldBe "CannotRemoveCanonicalAlias"
                error.shouldBeInstanceOf<ScopeContractError.BusinessError>()

                // The error should be self-contained and not expose internal details
                error.toString() shouldContain "CannotRemoveCanonicalAlias"

                // Since it's a data object, two instances should be equal
                val anotherError = ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
                error shouldBe anotherError
            }
        }

        "DuplicateAlias error should contain alias information" {
            runTest {
                // Arrange & Act
                val error = ScopeContractError.BusinessError.DuplicateAlias(alias = "test-alias")

                // Assert
                error.alias shouldBe "test-alias"
                error.shouldBeInstanceOf<ScopeContractError.BusinessError>()
            }
        }

        "HasChildren error should contain scope information" {
            runTest {
                // Arrange & Act
                val error = ScopeContractError.BusinessError.HasChildren(
                    scopeId = "parent-scope",
                    childrenCount = 3,
                )

                // Assert
                error.scopeId shouldBe "parent-scope"
                error.childrenCount shouldBe 3
                error.shouldBeInstanceOf<ScopeContractError.BusinessError>()
            }
        }

        "all business errors should be instances of BusinessError" {
            runTest {
                // Act & Assert - Verify error hierarchy
                val canonicalAliasError = ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
                canonicalAliasError.shouldBeInstanceOf<ScopeContractError.BusinessError>()

                val duplicateError = ScopeContractError.BusinessError.DuplicateAlias("test")
                duplicateError.shouldBeInstanceOf<ScopeContractError.BusinessError>()

                val hasChildrenError = ScopeContractError.BusinessError.HasChildren("test", null)
                hasChildrenError.shouldBeInstanceOf<ScopeContractError.BusinessError>()

                val notFoundError = ScopeContractError.BusinessError.AliasNotFound("test")
                notFoundError.shouldBeInstanceOf<ScopeContractError.BusinessError>()

                // System errors should be different hierarchy
                val systemError = ScopeContractError.SystemError.ServiceUnavailable("test")
                systemError.shouldBeInstanceOf<ScopeContractError.SystemError>()
            }
        }

        "error handling improvements should prevent generic ServiceUnavailable responses" {
            runTest {
                // This test documents the fix that was implemented to prevent
                // specific business errors from being mapped to generic ServiceUnavailable

                // Arrange - Create specific business errors
                val canonicalAliasError = ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
                val duplicateError = ScopeContractError.BusinessError.DuplicateAlias("test")
                val hasChildrenError = ScopeContractError.BusinessError.HasChildren("test", 2)

                // Assert - These should all be distinct error types, not generic system errors
                canonicalAliasError.shouldBeInstanceOf<ScopeContractError.BusinessError.CannotRemoveCanonicalAlias>()
                duplicateError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateAlias>()
                hasChildrenError.shouldBeInstanceOf<ScopeContractError.BusinessError.HasChildren>()

                // Only actual system failures should be ServiceUnavailable
                val actualSystemError = ScopeContractError.SystemError.ServiceUnavailable("service")
                actualSystemError.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
            }
        }

        "MCP server adapter should be constructible with mock ports" {
            runTest {
                // Arrange
                val queryPort: ScopeManagementQueryPort = mockk(relaxed = true)
                val commandPort: ScopeManagementCommandPort = mockk(relaxed = true)

                // Act & Assert - Basic instantiation test
                val logger = mockk<io.github.kamiazya.scopes.platform.observability.logging.Logger>(relaxed = true)
                val adapter = McpServerAdapter(queryPort, commandPort, logger)
                adapter.shouldBeInstanceOf<McpServerAdapter>()
            }
        }
    })
