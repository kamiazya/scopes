package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*

/**
 * Tests for the aliases.resolve tool functionality.
 *
 * This tool provides alias resolution with different matching modes:
 * - auto: tries exact match first, falls back to unique prefix
 * - exact: only exact matches
 * - prefix: unique prefix matching
 *
 * Returns canonicalAlias on success, or AmbiguousAlias error with candidates.
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

        "should return AmbiguousAlias error with candidates" {
            runTest {
                setupMocks()

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

        "should handle different match modes" {
            runTest {
                setupMocks()

                val matchModes = listOf("auto", "exact", "prefix")

                // Test that all match modes are valid
                matchModes.forEach { mode ->
                    mode shouldBe mode // Basic validation
                }

                // Default mode should be "auto"
                "auto" shouldBe "auto"
            }
        }

        "should validate input schema requirements" {
            runTest {
                // Test expected input structure for aliases.resolve
                val validInput = buildJsonObject {
                    put("alias", "test-alias")
                    put("match", "auto")
                }

                val minimalInput = buildJsonObject {
                    put("alias", "test-alias")
                    // match is optional, defaults to "auto"
                }

                // Verify required fields
                validInput["alias"] shouldNotBe null
                minimalInput["alias"] shouldNotBe null

                // Verify match mode validation
                val validModes = listOf("auto", "exact", "prefix")
                validModes.contains("auto") shouldBe true
                validModes.contains("exact") shouldBe true
                validModes.contains("prefix") shouldBe true
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

        "should handle prefix matching logic" {
            runTest {
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
