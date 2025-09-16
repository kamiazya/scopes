package io.github.kamiazya.scopes.interfaces.mcp.resources.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.resources.ResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.support.ResourceHelpers
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult

/**
 * Resource handler for scope details.
 *
 * Provides detailed information about a scope in JSON format.
 */
class ScopeDetailsResourceHandler : ResourceHandler {

    override val uriPattern: String = "scopes:/scope/{canonicalAlias}"

    override val name: String = "Scope Details (JSON)"

    override val description: String = "Scope details by canonical alias using the standard object shape"

    override val mimeType: String = "application/json"

    override suspend fun read(req: ReadResourceRequest, ports: Ports, services: Services): ReadResourceResult {
        val uri = req.uri
        val prefix = "scopes:/scope/"
        val alias = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else ""

        services.logger.debug("Reading scope details for alias: $alias")

        if (alias.isBlank()) {
            return ResourceHelpers.createErrorResourceResult(
                uri = uri,
                code = -32602,
                message = "Missing or invalid alias in resource URI",
            )
        }

        val result = ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))

        return when (result) {
            is Either.Left -> services.errors.mapContractErrorToResource(uri, result.value)
            is Either.Right -> {
                val scope = result.value
                ResourceHelpers.createScopeDetailsResult(uri, scope)
            }
        }
    }
}
