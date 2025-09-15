package io.github.kamiazya.scopes.interfaces.cli.commands

import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.mcp.adapters.McpServerAdapter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class McpCommand :
    ScopesCliktCommand(name = "mcp", help = "Run MCP server (stdio)"),
    KoinComponent {
    private val mcpServerAdapter: McpServerAdapter by inject()

    override fun run() {
        try {
            mcpServerAdapter.runStdio()
        } finally {
            // Cleanup handled by mcpServerAdapter internally
        }
    }
}
