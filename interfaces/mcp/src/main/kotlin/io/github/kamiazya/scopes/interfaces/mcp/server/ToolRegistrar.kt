package io.github.kamiazya.scopes.interfaces.mcp.server

import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking

/**
 * Registers multiple tool handlers with the MCP server.
 * 
 * This class bridges the suspend-based tool handler interface with
 * the synchronous MCP SDK tool registration API.
 */
class ToolRegistrar(
    private val handlers: List<ToolHandler>,
    private val ctxFactory: () -> Pair<Ports, Services>,
) : ServerRegistrar {
    
    override fun register(server: Server) {
        handlers.forEach { handler ->
            server.addTool(
                name = handler.name,
                description = handler.description,
                inputSchema = handler.input,
                outputSchema = handler.output,
            ) { req ->
                val (ports, services) = ctxFactory()
                runBlocking {
                    handler.handle(
                        ToolContext(
                            args = req.arguments,
                            ports = ports,
                            services = services,
                        )
                    )
                }
            }
        }
    }
}