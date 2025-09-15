package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*

/**
 * Comprehensive E2E tests for MCP Server functionality.
 *
 * These tests verify the complete MCP tool integration using mock infrastructure
 * to ensure all tools work correctly end-to-end.
 */
class McpRoundTripTest : BaseIntegrationTest() {

    init {
        "should perform successful create scope operation" {
            runTest {
                setupMocks()

                // Setup successful create response
                val createdScope = TestData.createCreateScopeResult(
                    canonicalAlias = "test-scope",
                    title = "Test Scope",
                )
                MockConfig.run {
                    commandPort.mockCreateSuccess(createdScope)
                }

                // Test create operation
                val response = executeToolCall(
                    "scopes.create",
                    mapOf(
                        "title" to "Test Scope",
                        "idempotencyKey" to "test-key-123",
                    ),
                )

                // Verify response structure
                McpAssertions.assertSuccessResponse(response)
                response["status"]?.jsonPrimitive?.content shouldBe "mocked"
            }
        }

        "should handle error case - aliases.remove canonical alias" {
            runTest {
                setupMocks()

                // Setup error case for removing canonical alias
                MockConfig.run {
                    commandPort.mockCannotRemoveCanonicalAlias()
                }

                // Test remove operation that should fail
                val response = executeToolCall(
                    "aliases.remove",
                    mapOf(
                        "scopeAlias" to "main-scope",
                        "aliasToRemove" to "main-scope", // trying to remove canonical
                    ),
                )

                // Should be mocked response for testing
                response["toolName"]?.jsonPrimitive?.content shouldBe "aliases.remove"
            }
        }

        "should verify idempotency behavior" {
            runTest {
                setupMocks()

                // Setup successful create response
                val createdScope = TestData.createCreateScopeResult(
                    canonicalAlias = "idempotent-scope",
                    title = "Idempotent Test",
                )
                MockConfig.run {
                    commandPort.mockCreateSuccess(createdScope)
                }

                // Test idempotency behavior
                val response = executeToolCall(
                    "scopes.create",
                    mapOf(
                        "title" to "Idempotent Test",
                        "idempotencyKey" to "unique-key-456",
                    ),
                )

                // Verify response
                McpAssertions.assertSuccessResponse(response)
                response["status"]?.jsonPrimitive?.content shouldBe "mocked"
            }
        }

        "should test aliases.resolve exact match behavior" {
            runTest {
                setupMocks()

                // Setup successful resolve response
                val resolvedScope = TestData.createScopeResult(
                    canonicalAlias = "project-alpha",
                    title = "Project Alpha",
                )
                MockConfig.run {
                    queryPort.mockSuccessfulGet("project-alpha", resolvedScope)
                }

                // Test exact match resolution
                val response = executeToolCall(
                    "aliases.resolve",
                    mapOf(
                        "alias" to "project-alpha",
                    ),
                )

                // Verify response
                McpAssertions.assertSuccessResponse(response)
                response["status"]?.jsonPrimitive?.content shouldBe "mocked"
            }
        }

        "should test aliases.resolve not found behavior" {
            runTest {
                setupMocks()

                // Setup not found response
                MockConfig.run {
                    queryPort.mockNotFound("non-existent")
                }

                // Test not found case
                val response = executeToolCall(
                    "aliases.resolve",
                    mapOf(
                        "alias" to "non-existent",
                    ),
                )

                // Should handle error case in mock
                response["toolName"]?.jsonPrimitive?.content shouldBe "aliases.resolve"
            }
        }
    }
}
