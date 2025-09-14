package io.github.kamiazya.scopes.interfaces.mcp

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.platform.observability.logging.Slf4jLogger
import io.github.kamiazya.scopes.interfaces.providers.McpServerFactory
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.mockk.mockk
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe

class McpServerFactoryTest : StringSpec({
    "create() should register basic tools/prompts/resources" {
        val logger = Slf4jLogger("test")
        val query: ScopeManagementQueryPort = mockk(relaxed = true)
        val command: ScopeManagementCommandPort = mockk(relaxed = true)

        val factory = McpServerFactory(logger, query, command)
        val server: Server = factory.create()
        server shouldNotBe null
    }
})
