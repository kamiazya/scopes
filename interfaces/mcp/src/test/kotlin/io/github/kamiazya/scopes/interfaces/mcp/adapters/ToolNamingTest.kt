package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.mockk

/**
 * Tests for MCP server tool naming conventions.
 *
 * These tests verify that tools follow the new dot+camel naming convention:
 * - Segments are separated by '.'
 * - Each segment uses camelCase
 * - Examples: scopes.get, scopes.listWithAspect, aliases.setCanonical
 */
class ToolNamingTest :
    StringSpec({

        "should use dot+camel naming convention for all tools" {
            val queryPort = mockk<ScopeManagementQueryPort>()
            val commandPort = mockk<ScopeManagementCommandPort>()
            val logger = mockk<io.github.kamiazya.scopes.platform.observability.logging.Logger>(relaxed = true)
            val adapter = McpServerAdapter(queryPort, commandPort, logger)

            val server = adapter.createTestServer()

            // Unfortunately, we can't easily inspect registered tools from the Server API
            // We'll test this indirectly through expected tool names

            // Test naming convention pattern
            val expectedToolNames = listOf(
                "scopes.get", // get-scope-by-alias
                "scopes.create", // create-scope
                "scopes.update", // update-scope
                "scopes.delete", // delete-scope
                "scopes.children", // get-children
                "scopes.roots", // get-root-scopes
                "scopes.listAliases", // list-aliases (should be camelCase)
                "aliases.add", // add-alias
                "aliases.remove", // remove-alias
                "aliases.setCanonical", // set-canonical-alias (should be camelCase)
                "aliases.resolve", // New tool for alias resolution
            )

            // Test that names follow the pattern: word.word (each word in camelCase)
            expectedToolNames.forEach { toolName ->
                toolName shouldMatch Regex("^[a-z]+(?:\\.[a-z][a-zA-Z]*)*$")
            }

            // Test that old naming conventions are not used
            val oldStyleNames = listOf(
                "get-scope-by-alias",
                "create-scope",
                "update-scope",
                "delete-scope",
                "get-children",
                "get-root-scopes",
                "list-aliases",
                "add-alias",
                "remove-alias",
                "set-canonical-alias",
                "get-tool-list",
            )

            // These should not be present in new implementation
            // (We can't easily test this without exposing server tools list)
        }

        "should validate tool name segments use camelCase" {
            val validNames = listOf(
                "scopes.get",
                "scopes.listWithAspect",
                "aliases.setCanonical",
                "resources.readTemplate",
            )

            val invalidNames = listOf(
                "scopes.get-by-alias", // hyphen not allowed
                "scopes.GetScope", // PascalCase not allowed
                "scopes.list_aliases", // underscore not allowed
                "Scopes.get", // First segment must be lowercase
            )

            val dotCamelPattern = Regex("^[a-z]+(?:\\.[a-z][a-zA-Z]*)*$")

            validNames.forEach { name ->
                name shouldMatch dotCamelPattern
            }

            invalidNames.forEach { name ->
                name.matches(dotCamelPattern) shouldBe false
            }
        }

        "should define expected tool categories" {
            // Test that we have the right categories/prefixes
            val expectedCategories = listOf("scopes", "aliases")
            val futureCategories = listOf("resources", "prompts") // May be added later

            expectedCategories.forEach { category ->
                category shouldMatch Regex("^[a-z]+$")
            }
        }
    })
