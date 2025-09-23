package io.github.kamiazya.scopes.interfaces.mcp.schema

import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeGetToolHandler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class ToolSchemasSnapshotTest :
    StringSpec({
        val codec = createArgumentCodec()

        "ScopeGet input schema snapshot (canonicalized)" {
            val handler = ScopeGetToolHandler()
            val actual = codec.canonicalizeJson(handler.input.properties)

            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("required", buildJsonArray { add("alias") })
                    putJsonObject("properties") {
                        putJsonObject("alias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Scope alias to look up")
                        }
                    }
                },
            )

            actual shouldBe expected
        }

        "ScopeGet output schema snapshot (canonicalized)" {
            val handler = ScopeGetToolHandler()
            val actual = codec.canonicalizeJson(handler.output.properties)

            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("title") { put("type", "string") }
                        putJsonObject("description") { put("type", "string") }
                        putJsonObject("createdAt") { put("type", "string") }
                        putJsonObject("updatedAt") { put("type", "string") }
                    }
                    put("required", Json.parseToJsonElement("[\"canonicalAlias\",\"title\",\"createdAt\",\"updatedAt\"]"))
                },
            )

            actual shouldBe expected
        }
    })
