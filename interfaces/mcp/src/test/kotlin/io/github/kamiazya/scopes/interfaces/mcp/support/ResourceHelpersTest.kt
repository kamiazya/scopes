package io.github.kamiazya.scopes.interfaces.mcp.support

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResourceHelpersTest :
    StringSpec({
        "scopeLinks returns three standard links" {
            val links = ResourceHelpers.scopeLinks("a")
            links.map { it["rel"]?.toString() } shouldContainExactly listOf("\"self\"", "\"tree\"", "\"tree.md\"")
            links.map { it["uri"]?.toString() } shouldContainExactly listOf("\"scopes:/scope/a\"", "\"scopes:/tree/a\"", "\"scopes:/tree.md/a\"")
        }

        "treeLinks returns two standard links with depth" {
            val links = ResourceHelpers.treeLinks("a", 3)
            links.map { it["rel"]?.toString() } shouldContainExactly listOf("\"self\"", "\"scope\"")
            links.map { it["uri"]?.toString() } shouldContainExactly listOf("\"scopes:/tree/a?depth=3\"", "\"scopes:/scope/a\"")
        }

        "extractAlias picks alias after prefix" {
            ResourceHelpers.extractAlias("scopes:/scope/foo", "scopes:/scope/") shouldBe "foo"
            ResourceHelpers.extractAlias("invalid://bar", "scopes:/scope/") shouldBe ""
        }

        "parseTreeAlias with maxDepth clamps depth" {
            val (alias, depth) = ResourceHelpers.parseTreeAlias("root?depth=10", maxDepth = 3)
            alias shouldBe "root"
            depth shouldBe 3
        }
    })
