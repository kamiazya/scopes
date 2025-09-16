package io.github.kamiazya.scopes.interfaces.mcp.prompts

import io.github.kamiazya.scopes.interfaces.mcp.server.ServerRegistrar
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking

/**
 * Registrar for MCP prompt providers.
 *
 * This class registers all prompt providers with the MCP server, providing
 * the necessary execution environment for each provider.
 */
class PromptRegistrar(private val providers: List<PromptProvider>) : ServerRegistrar {

    override fun register(server: Server) {
        providers.forEach { provider ->
            server.addPrompt(
                name = provider.name,
                description = provider.description,
                arguments = provider.arguments,
            ) { req ->
                val args = req.arguments ?: emptyMap()
                runBlocking {
                    provider.get(args.mapValues { it.value?.toString() })
                }
            }
        }
    }
}
