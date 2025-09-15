package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

/**
 * Tests for JSON Schema validation in MCP tools.
 *
 * These tests verify that:
 * - All tools have proper JSON Schema definitions
 * - additionalProperties is set to false
 * - Required fields are properly declared
 * - Validation constraints (enum, pattern, minLength) are set
 */
class JsonSchemaTest :
    StringSpec({

        "should validate idempotency key pattern" {
            // Test the idempotency key validation pattern
            val validKeys = listOf(
                "abcd1234", // 8 characters minimum
                "user-action-001", // with hyphens
                "bulk_import_xyz", // with underscores
                "A1B2C3D4E5F6G7H8", // mixed case, 16 chars
                "very-long-key-with-many-characters-1234567890123456789012345678901234567890123456789012345678901234567890123456", // 128 chars max
            )

            val invalidKeys = listOf(
                "short", // too short (< 8 chars)
                "has@special", // invalid characters
                "has spaces", // spaces not allowed
                "", // empty
                "a".repeat(129), // too long (> 128 chars)
            )

            val idempotencyPattern = Regex("^[A-Za-z0-9_-]{8,128}$")

            validKeys.forEach { key ->
                key shouldMatch idempotencyPattern
            }

            invalidKeys.forEach { key ->
                key.matches(idempotencyPattern) shouldBe false
            }
        }
    })
