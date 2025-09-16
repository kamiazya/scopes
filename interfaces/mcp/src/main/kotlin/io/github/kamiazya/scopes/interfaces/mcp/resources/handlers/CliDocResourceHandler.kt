package io.github.kamiazya.scopes.interfaces.mcp.resources.handlers

import io.github.kamiazya.scopes.interfaces.mcp.resources.ResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.support.ResourceHelpers
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult

/**
 * Resource handler for CLI documentation.
 *
 * Provides quick reference documentation for the Scopes CLI.
 */
class CliDocResourceHandler : ResourceHandler {

    override val uriPattern: String = "scopes:/docs/cli"

    override val name: String = "CLI Quick Reference"

    override val description: String = "Scopes CLI quick reference"

    override val mimeType: String = "text/markdown"

    override suspend fun read(req: ReadResourceRequest, ports: Ports, services: Services): ReadResourceResult {
        services.logger.debug("Reading CLI documentation resource")

        val text = "See repository docs/reference/cli-quick-reference.md"

        return ResourceHelpers.createSimpleTextResult(
            uri = req.uri,
            text = text,
            mimeType = mimeType,
        )
    }
}
