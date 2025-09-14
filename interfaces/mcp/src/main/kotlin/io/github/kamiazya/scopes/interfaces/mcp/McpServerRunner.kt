package io.github.kamiazya.scopes.interfaces.providers

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered

class McpServerRunner(
    private val factory: McpServerFactory,
    private val logger: Logger,
) {
    fun runStdioBlocking() {
        val server = factory.create()
        val transport = StdioServerTransport(
            inputStream = java.io.BufferedInputStream(System.`in`),
            outputStream = java.io.PrintStream(System.out, true),
        )
        runBlocking {
            server.connect(transport)
            logger.info("MCP server started on stdio")
            // Keep process alive; MCP client typically terminates the process when done.
            // If interrupted, exit gracefully.
            try {
                while (true) kotlinx.coroutines.delay(60_000)
            } catch (_: Throwable) {
                // exit
            }
            logger.info("MCP server stopping")
        }
    }
}
