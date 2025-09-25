package io.github.kamiazya.scopes.interfaces.mcp.schema

import io.github.kamiazya.scopes.interfaces.mcp.support.IdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesAddToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesRemoveToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesSetCanonicalCamelToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeCreateToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeDeleteToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeUpdateToolHandler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class InputSchemasSnapshotTest :
    StringSpec({
        val codec = createArgumentCodec()
        val pattern = IdempotencyService.IDEMPOTENCY_KEY_PATTERN_STRING

        "ScopeCreate input schema snapshot" {
            val h = ScopeCreateToolHandler()
            val actual = codec.canonicalizeJson(h.input.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("required", Json.parseToJsonElement("[\"title\"]"))
                    putJsonObject("properties") {
                        putJsonObject("title") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Scope title")
                        }
                        putJsonObject("description") {
                            put("type", "string")
                            put("description", "Optional scope description")
                        }
                        putJsonObject("parentAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Parent scope alias (optional)")
                        }
                        putJsonObject("customAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Custom alias instead of generated one (optional)")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", pattern)
                            put("description", "Idempotency key to prevent duplicate operations")
                        }
                    }
                },
            )
            actual shouldBe expected
        }

        "ScopeUpdate input schema snapshot" {
            val h = ScopeUpdateToolHandler()
            val actual = codec.canonicalizeJson(h.input.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("required", Json.parseToJsonElement("[\"alias\"]"))
                    putJsonObject("properties") {
                        putJsonObject("alias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Scope alias to update")
                        }
                        putJsonObject("title") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "New title (optional)")
                        }
                        putJsonObject("description") {
                            put("type", "string")
                            put("description", "New description (optional)")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", pattern)
                            put("description", "Idempotency key to prevent duplicate operations")
                        }
                    }
                },
            )
            actual shouldBe expected
        }

        "ScopeDelete input schema snapshot" {
            val h = ScopeDeleteToolHandler()
            val actual = codec.canonicalizeJson(h.input.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("required", Json.parseToJsonElement("[\"alias\"]"))
                    putJsonObject("properties") {
                        putJsonObject("alias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Scope alias to delete")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", pattern)
                            put("description", "Idempotency key to prevent duplicate operations")
                        }
                    }
                },
            )
            actual shouldBe expected
        }

        "AliasesSetCanonical input schema snapshot" {
            val h = AliasesSetCanonicalCamelToolHandler()
            val actual = codec.canonicalizeJson(h.input.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("required", Json.parseToJsonElement("[\"scopeAlias\",\"newCanonicalAlias\"]"))
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Existing scope alias")
                        }
                        putJsonObject("newCanonicalAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Alias to make canonical")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", pattern)
                            put("description", "Idempotency key to prevent duplicate operations")
                        }
                    }
                },
            )
            actual shouldBe expected
        }

        "AliasesAdd input schema snapshot" {
            val h = AliasesAddToolHandler()
            val actual = codec.canonicalizeJson(h.input.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("required", Json.parseToJsonElement("[\"scopeAlias\",\"newAlias\"]"))
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Existing scope alias")
                        }
                        putJsonObject("newAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "New alias to add")
                        }
                        putJsonObject("makeCanonical") {
                            put("type", "boolean")
                            put("description", "Make this the canonical alias (optional, default false)")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", pattern)
                            put("description", "Idempotency key to prevent duplicate operations")
                        }
                    }
                },
            )
            actual shouldBe expected
        }

        "AliasesRemove input schema snapshot" {
            val h = AliasesRemoveToolHandler()
            val actual = codec.canonicalizeJson(h.input.properties)
            val expected = codec.canonicalizeJson(
                buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("required", Json.parseToJsonElement("[\"scopeAlias\",\"aliasToRemove\"]"))
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Scope alias to look up")
                        }
                        putJsonObject("aliasToRemove") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Alias to remove (cannot be canonical)")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", pattern)
                            put("description", "Idempotency key to prevent duplicate operations")
                        }
                    }
                },
            )
            actual shouldBe expected
        }
    })
