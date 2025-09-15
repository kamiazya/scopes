package io.github.kamiazya.scopes.interfaces.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.Server

/**
 * Interface for components that register capabilities with the MCP Server.
 * 
 * This allows different types of registrars (tools, resources, prompts)
 * to be composed together cleanly.
 */
fun interface ServerRegistrar {
    /**
     * Register capabilities with the given MCP server.
     * 
     * @param server The MCP server to register with
     */
    fun register(server: Server)
}