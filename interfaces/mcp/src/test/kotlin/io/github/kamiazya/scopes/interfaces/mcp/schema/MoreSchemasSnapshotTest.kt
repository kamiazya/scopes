package io.github.kamiazya.scopes.interfaces.mcp.schema

import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopesListAliasesToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopesRootsToolHandler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class MoreSchemasSnapshotTest :
    StringSpec({
        val codec = createArgumentCodec()

        "ScopesRoots output schema snapshot" {
            val handler = ScopesRootsToolHandler()
            val actual = codec.canonicalizeJson(handler.output.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("roots") {
                            put("type", "array")
                            putJsonObject("items") {
                                put("type", "object")
                                put("additionalProperties", false)
                                putJsonObject("properties") {
                                    putJsonObject("canonicalAlias") { put("type", "string") }
                                    putJsonObject("title") { put("type", "string") }
                                    putJsonObject("description") { put("type", "string") }
                                }
                                put(
                                    "required",
                                    buildJsonArray {
                                        add("canonicalAlias")
                                        add("title")
                                    },
                                )
                            }
                        }
                    }
                    put("required", buildJsonArray { add("roots") })
                },
            )
            actual shouldBe expected
        }

        "ScopesListAliases output schema snapshot" {
            val handler = ScopesListAliasesToolHandler()
            val actual = codec.canonicalizeJson(handler.output.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") { put("type", "string") }
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("aliases") {
                            put("type", "array")
                            putJsonObject("items") {
                                put("type", "object")
                                put("additionalProperties", false)
                                putJsonObject("properties") {
                                    putJsonObject("aliasName") { put("type", "string") }
                                    putJsonObject("isCanonical") { put("type", "boolean") }
                                    putJsonObject("aliasType") { put("type", "string") }
                                }
                                put(
                                    "required",
                                    buildJsonArray {
                                        add("aliasName")
                                        add("isCanonical")
                                    },
                                )
                            }
                        }
                    }
                    put(
                        "required",
                        buildJsonArray {
                            add("scopeAlias")
                            add("aliases")
                        },
                    )
                },
            )
            actual shouldBe expected
        }
    })
