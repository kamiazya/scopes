package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class ScopeGetToolHandlerTest :
    StringSpec({
        "should have correct tool metadata" {
            val handler = ScopeGetToolHandler()

            handler.name shouldBe "scopes.get"
            handler.description shouldContain "Get a scope by alias"
            handler.input shouldNotBe null
            handler.output shouldNotBe null
        }
    })
