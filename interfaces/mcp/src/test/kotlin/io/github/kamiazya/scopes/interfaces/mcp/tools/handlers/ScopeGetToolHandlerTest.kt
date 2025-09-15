package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import io.github.kamiazya.scopes.interfaces.mcp.adapters.BaseIntegrationTest
import io.github.kamiazya.scopes.interfaces.mcp.support.DefaultArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.DefaultErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.DefaultIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk

class ScopeGetToolHandlerTest : BaseIntegrationTest() {
    init {
        "should have correct tool metadata" {
            val handler = ScopeGetToolHandler()

            handler.name shouldBe "scopes.get"
            handler.description shouldContain "Get a scope by alias"
            handler.input shouldNotBe null
            handler.output shouldNotBe null
        }
    }

    private fun createServices(): Services {
        val argumentCodec = DefaultArgumentCodec()
        val errorMapper = DefaultErrorMapper()
        val idempotencyService = DefaultIdempotencyService(argumentCodec)
        val logger = mockk<io.github.kamiazya.scopes.platform.observability.logging.Logger>(relaxed = true)

        return Services(
            errors = errorMapper,
            idempotency = idempotencyService,
            codec = argumentCodec,
            logger = logger,
        )
    }
}
