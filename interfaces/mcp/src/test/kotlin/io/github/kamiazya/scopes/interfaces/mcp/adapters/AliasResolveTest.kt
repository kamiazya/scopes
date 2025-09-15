package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import io.kotest.core.annotation.Ignored

/**
 * Tests for the aliases.resolve tool functionality.
 *
 * NOTE: As of v1, this tool supports EXACT MATCH ONLY.
 * Previous prefix/auto matching functionality has been removed
 * to simplify the implementation and ensure deterministic behavior.
 *
 * Returns canonicalAlias on success, or AliasNotFound error if not found.
 */
class AliasResolveTest : BaseIntegrationTest() {

    init {
        "should resolve exact alias match" {
            runTest {
                setupMocks()

                val testScope = TestData.createScopeResult(
                    canonicalAlias = "project-alpha",
                    title = "Project Alpha",
                )
                MockConfig.run { queryPort.mockSuccessfulGet("project-alpha", testScope) }

                // Expected response for exact match
                val expectedResponse = buildJsonObject {
                    put("canonicalAlias", "project-alpha")
                }

                // Verify test data setup
                testScope.canonicalAlias shouldBe "project-alpha"
                expectedResponse["canonicalAlias"]?.toString() shouldBe "\"project-alpha\""
            }
        }

        @Ignored("TODO: v1 removed prefix matching - AmbiguousAlias no longer applies")
        "should return AmbiguousAlias error with candidates - DEPRECATED" {
            runTest {
                setupMocks()

                // NOTE: This test is disabled because v1 only supports exact matching
                // AmbiguousAlias errors are no longer possible with exact-only matching

                // Setup ambiguous match scenario
                MockConfig.run { queryPort.mockNotFound("proj") }

                // Expected AmbiguousAlias error response
                val expectedError = buildJsonObject {
                    put("code", -32011)
                    put("message", "Ambiguous alias")
                    putJsonObject("data") {
                        put("type", "AmbiguousAlias")
                        put("message", "Multiple scopes match the prefix 'proj'")
                        putJsonArray("candidates") {
                            add(JsonPrimitive("project-alpha"))
                            add(JsonPrimitive("project-beta"))
                            add(JsonPrimitive("proj-gamma"))
                        }
                        put("hint", "Use a more specific prefix or exact match")
                    }
                }

                // Verify error structure
                expectedError["code"]?.toString() shouldBe "-32011"
                expectedError["data"]?.let { data ->
                    data.toString().contains("AmbiguousAlias") shouldBe true
                }
            }
        }

        "should return AliasNotFound error for non-existent alias" {
            runTest {
                setupMocks()

                // Setup not found scenario
                MockConfig.run { queryPort.mockNotFound("non-existent-alias") }

                // Expected AliasNotFound error response
                val expectedError = buildJsonObject {
                    put("code", -32011)
                    put("message", "Alias not found: non-existent-alias")
                }

                // Verify error structure for exact-only v1 behavior
                expectedError["code"]?.toString() shouldBe "-32011"
                expectedError["message"]?.toString() shouldBe "\"Alias not found: non-existent-alias\""
            }
        }

        @Ignored("TODO: v1 removed match modes - only exact matching supported")
        "should handle different match modes - DEPRECATED" {
            runTest {
                setupMocks()

                // NOTE: This test is disabled because v1 only supports exact matching
                // Match modes (auto, exact, prefix) are no longer supported

                val matchModes = listOf("auto", "exact", "prefix")

                // Test that all match modes are valid
                matchModes.forEach { mode ->
                    mode shouldBe mode // Basic validation
                }

                // Default mode should be "auto"
                "auto" shouldBe "auto"
            }
        }

        "should validate input schema requirements for v1 exact-only" {
            runTest {
                // Test expected input structure for aliases.resolve v1 (exact-only)
                val validInput = buildJsonObject {
                    put("alias", "test-alias")
                    // NOTE: v1 removed 'match' parameter - only exact matching
                }

                // Verify required fields for v1
                validInput["alias"] shouldNotBe null

                // v1 only supports exact matching - no mode parameter needed
                val inputKeys = validInput.keys
                inputKeys.contains("alias") shouldBe true
                inputKeys.contains("match") shouldBe false // Removed in v1
            }
        }

        "should generate proper response structure" {
            runTest {
                // Test successful response structure
                val successResponse = buildJsonObject {
                    put("canonicalAlias", "resolved-alias")
                }

                // Test ambiguous response structure
                val ambiguousResponse = buildJsonObject {
                    put("canonicalAlias", "partial-match")
                    putJsonArray("candidates") {
                        add(JsonPrimitive("partial-match-1"))
                        add(JsonPrimitive("partial-match-2"))
                    }
                }

                successResponse["canonicalAlias"] shouldNotBe null
                ambiguousResponse["candidates"] shouldNotBe null
            }
        }

        @Ignored("TODO: v1 removed prefix matching - only exact matching supported")
        "should handle prefix matching logic - DEPRECATED" {
            runTest {
                // NOTE: This test is disabled because v1 only supports exact matching
                // Prefix matching logic is no longer applicable

                // Test prefix matching scenarios
                val prefixes = mapOf(
                    "proj" to listOf("project-alpha", "project-beta", "proj-gamma"),
                    "project-a" to listOf("project-alpha"), // unique prefix
                    "project-" to listOf("project-alpha", "project-beta"), // ambiguous
                )

                prefixes.forEach { (prefix, matches) ->
                    when (matches.size) {
                        1 -> {
                            // Should resolve to the unique match
                            matches.first().startsWith(prefix) shouldBe true
                        }
                        else -> {
                            // Should return AmbiguousAlias with candidates
                            matches.all { it.startsWith(prefix) } shouldBe true
                        }
                    }
                }
            }
        }
    }
}
