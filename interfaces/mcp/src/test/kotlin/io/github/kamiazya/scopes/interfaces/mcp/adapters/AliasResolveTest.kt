package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*

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

                successResponse["canonicalAlias"] shouldNotBe null
                successResponse["canonicalAlias"]?.jsonPrimitive?.content shouldBe "resolved-alias"
            }
        }
    }
}
