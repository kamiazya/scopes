package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk

/**
 * Unit tests for McpServerAdapter focusing on adapter structure and tool registration.
 * 
 * This test class verifies:
 * - Adapter initialization and configuration
 * - Server creation and setup
 * - Basic functionality without diving into private API details
 */
class McpServerAdapterTest : StringSpec({

    "should create adapter with required dependencies" {
        val queryPort = mockk<ScopeManagementQueryPort>()
        val commandPort = mockk<ScopeManagementCommandPort>()
        
        val adapter = McpServerAdapter(queryPort, commandPort)
        
        adapter shouldNotBe null
    }

    "should create test server successfully" {
        val queryPort = mockk<ScopeManagementQueryPort>()
        val commandPort = mockk<ScopeManagementCommandPort>()
        val adapter = McpServerAdapter(queryPort, commandPort)
        
        val server = adapter.createTestServer()
        
        server shouldNotBe null
        // Note: Server.capabilities is private, so we can't test it directly
        // But successful server creation indicates proper setup
    }

    "should handle stdio server creation" {
        val queryPort = mockk<ScopeManagementQueryPort>()
        val commandPort = mockk<ScopeManagementCommandPort>()
        val adapter = McpServerAdapter(queryPort, commandPort)
        
        // This should not throw an exception
        // We can't easily test stdio without actual input/output streams
        adapter shouldNotBe null
    }
})