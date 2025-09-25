package io.github.kamiazya.scopes.interfaces.mcp.schema

import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasResolveToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeCreateToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeDeleteToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeUpdateToolHandler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class WriteToolsSchemasSnapshotTest :
    StringSpec({
        val codec = createArgumentCodec()

        "ScopeCreate output schema snapshot" {
            val h = ScopeCreateToolHandler()
            val actual = codec.canonicalizeJson(h.output.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("title") { put("type", "string") }
                        putJsonObject("description") { put("type", "string") }
                        putJsonObject("parentAlias") { put("type", "string") }
                        putJsonObject("createdAt") { put("type", "string") }
                    }
                    put("required", Json.parseToJsonElement("[\"canonicalAlias\",\"title\",\"createdAt\"]"))
                },
            )
            actual shouldBe expected
        }

        "ScopeUpdate output schema snapshot" {
            val h = ScopeUpdateToolHandler()
            val actual = codec.canonicalizeJson(h.output.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("title") { put("type", "string") }
                        putJsonObject("description") { put("type", "string") }
                        putJsonObject("updatedAt") { put("type", "string") }
                    }
                    put("required", Json.parseToJsonElement("[\"canonicalAlias\",\"title\",\"updatedAt\"]"))
                },
            )
            actual shouldBe expected
        }

        "ScopeDelete output schema snapshot" {
            val h = ScopeDeleteToolHandler()
            val actual = codec.canonicalizeJson(h.output.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("deleted") { put("type", "boolean") }
                    }
                    put("required", Json.parseToJsonElement("[\"canonicalAlias\",\"deleted\"]"))
                },
            )
            actual shouldBe expected
        }

        "AliasResolve output schema snapshot" {
            val h = AliasResolveToolHandler()
            val actual = codec.canonicalizeJson(h.output.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("alias") {
                            put("type", "string")
                            put("description", "The input alias provided by the user")
                        }
                        putJsonObject("canonicalAlias") {
                            put("type", "string")
                            put("description", "The canonical (normalized) alias of the scope")
                        }
                        putJsonObject("title") {
                            put("type", "string")
                            put("description", "The title of the resolved scope")
                        }
                    }
                    put("required", Json.parseToJsonElement("[\"alias\",\"canonicalAlias\",\"title\"]"))
                },
            )
            actual shouldBe expected
        }
    })
