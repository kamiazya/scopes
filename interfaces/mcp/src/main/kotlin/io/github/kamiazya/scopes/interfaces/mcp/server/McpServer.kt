package io.github.kamiazya.scopes.interfaces.mcp.server

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * MCP Server orchestrator that coordinates all server components.
 *
 * This is the main entry point for the MCP server, coordinating server creation,
 * component registration, and lifecycle management following Clean Architecture principles.
 */
class McpServer(
    private val serverBuilder: ServerBuilder,
    private val transportFactory: TransportFactory,
    private val registrars: List<ServerRegistrar>,
    private val logger: Logger,
) {

    private var server: Server? = null

    /**
     * Run the MCP server on stdio transport.
     * This is the main entry point for the MCP server.
     */
    fun runStdio(source: Source, sink: Sink) {
        val server = createServer()
        this.server = server
        val transport = transportFactory.stdio(source, sink)

        runBlocking {
            try {
                logger.info("Starting MCP server (stdio)")
                server.connect(transport)
                logger.info("MCP server disconnected")
            } catch (e: CancellationException) {
                logger.info("MCP server cancelled")
            } catch (e: Exception) {
                logger.error("Failed to run MCP server", throwable = e)
                throw e
            } finally {
                this@McpServer.server = null
            }
        }
    }

    /**
     * Create a server configured for production use.
     */
    fun createServer(): Server {
        val server = serverBuilder.build()
        registerComponents(server)
        return server
    }

    /**
     * Create a server configured for testing purposes.
     * This method exposes server configuration for integration tests.
     */
    fun createTestServer(): Server {
        val server = serverBuilder.buildTestServer()
        registerComponents(server)
        return server
    }

    /**
     * Register all components (tools, resources, prompts) with the server.
     */
    private fun registerComponents(server: Server) {
        logger.debug("Registering ${registrars.size} server components")
        registrars.forEach { registrar ->
            registrar.register(server)
        }
        logger.debug("Server components registered successfully")
    }

    /**
     * Notification methods for list_changed events.
     * Currently no-op as SDK API is TBD.
     */
    fun notifyToolsListChanged(): Boolean {
        logger.warn("tools list_changed notification is currently a no-op (SDK API TBD)")
        return false
    }

    fun notifyResourcesListChanged(): Boolean {
        logger.warn("resources list_changed notification is currently a no-op (SDK API TBD)")
        return false
    }

    fun notifyPromptsListChanged(): Boolean {
        logger.warn("prompts list_changed notification is currently a no-op (SDK API TBD)")
        return false
    }
}
