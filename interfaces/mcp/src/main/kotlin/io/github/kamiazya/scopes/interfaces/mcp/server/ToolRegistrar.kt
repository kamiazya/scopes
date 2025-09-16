package io.github.kamiazya.scopes.interfaces.mcp.server

import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlin.time.measureTimedValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Registers multiple tool handlers with the MCP server.
 *
 * This class bridges the suspend-based tool handler interface with
 * the synchronous MCP SDK tool registration API.
 */
@OptIn(ExperimentalUuidApi::class)
class ToolRegistrar(private val handlers: List<ToolHandler>, private val ctxFactory: () -> Pair<Ports, Services>) : ServerRegistrar {

    override fun register(server: Server) {
        handlers.forEach { handler ->
            server.addTool(
                name = handler.name,
                description = handler.description,
                inputSchema = handler.input,
                outputSchema = handler.output,
                toolAnnotations = handler.annotations,
            ) { req ->
                val (ports, services) = ctxFactory()
                val requestId = Uuid.random().toString()

                services.logger.debug("[$requestId] Starting tool execution: ${handler.name}")

                val (result, duration) = measureTimedValue {
                    runBlocking {
                        handler.handle(
                            ToolContext(
                                args = req.arguments,
                                ports = ports,
                                services = services,
                            ),
                        )
                    }
                }

                val status = if (result.isError == true) "failed" else "succeeded"
                services.logger.debug("[$requestId] Tool execution completed: ${handler.name} - $status in ${duration.inWholeMilliseconds}ms")

                result
            }
        }
    }
}
