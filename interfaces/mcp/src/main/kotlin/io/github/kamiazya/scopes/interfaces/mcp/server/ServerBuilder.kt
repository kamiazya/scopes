package io.github.kamiazya.scopes.interfaces.mcp.server

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions

/**
 * Builder for creating MCP servers with consistent configuration.
 *
 * This encapsulates server implementation details and capabilities configuration,
 * providing both production and test server creation.
 */
class ServerBuilder(
    private val name: String = "scopes",
    private val version: String = "0.1.0",
    private val capabilities: ServerCapabilities = ServerCapabilities(
        tools = ServerCapabilities.Tools(listChanged = true),
        resources = ServerCapabilities.Resources(subscribe = false, listChanged = true),
        prompts = ServerCapabilities.Prompts(listChanged = true),
    ),
) {

    /**
     * Build a production server with the configured settings.
     */
    fun build(): Server = Server(
        Implementation(name = name, version = version),
        ServerOptions(capabilities = capabilities),
    )

    /**
     * Build a test server with test-specific configuration.
     */
    fun buildTestServer(): Server = Server(
        Implementation(name = "$name-test", version = version),
        ServerOptions(capabilities = capabilities),
    )

    /**
     * Create a builder for test servers.
     */
    companion object {
        fun forTest(): ServerBuilder = ServerBuilder(name = "scopes-test")
    }
}
