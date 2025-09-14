package io.github.kamiazya.scopes.interfaces.cli.commands

import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.providers.McpServerRunner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class McpCommand : ScopesCliktCommand(name = "mcp", help = "Run MCP server (stdio)"), KoinComponent {
    private val runner: McpServerRunner by inject()

    override fun run() {
        runner.runStdioBlocking()
    }
}
