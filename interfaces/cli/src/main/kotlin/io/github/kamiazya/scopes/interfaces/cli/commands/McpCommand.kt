package io.github.kamiazya.scopes.interfaces.cli.commands

import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.mcp.server.McpServer
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class McpCommand :
    ScopesCliktCommand(name = "mcp", help = "Run MCP server (stdio)"),
    KoinComponent {
    private val mcpServer: McpServer by inject()

    override fun run() {
        try {
            mcpServer.runStdio(
                source = System.`in`.asSource().buffered(),
                sink = System.out.asSink().buffered()
            )
        } finally {
            // Cleanup handled by mcpServer internally
        }
    }
}
