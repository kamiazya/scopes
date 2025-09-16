package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import io.github.kamiazya.scopes.interfaces.mcp.support.ArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.ErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.IdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk

class ScopeGetToolHandlerTest : StringSpec({
    "should have correct tool metadata" {
        val handler = ScopeGetToolHandler()

        handler.name shouldBe "scopes.get"
        handler.description shouldContain "Get a scope by alias"
        handler.input shouldNotBe null
        handler.output shouldNotBe null
    }
})
