package io.github.kamiazya.scopes.interfaces.mcp.resources

import io.github.kamiazya.scopes.interfaces.mcp.server.ServerRegistrar
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking

/**
 * Registrar for MCP resource handlers.
 *
 * This class registers all resource handlers with the MCP server, providing
 * the necessary context and execution environment for each handler.
 */
class ResourceRegistrar(private val handlers: List<ResourceHandler>, private val contextFactory: () -> Pair<Ports, Services>) : ServerRegistrar {

    override fun register(server: Server) {
        handlers.forEach { handler ->
            server.addResource(
                uri = handler.uriPattern,
                name = handler.name,
                description = handler.description,
                mimeType = handler.mimeType,
            ) { req ->
                val (ports, services) = contextFactory()
                runBlocking {
                    handler.read(req, ports, services)
                }
            }
        }
    }
}
