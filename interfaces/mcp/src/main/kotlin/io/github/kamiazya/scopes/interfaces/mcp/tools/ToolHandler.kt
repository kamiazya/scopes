package io.github.kamiazya.scopes.interfaces.mcp.tools

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.interfaces.mcp.support.ArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.ErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.IdempotencyService
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonElement

/**
 * Interface for handling individual MCP tools.
 *
 * Each tool handler defines its schema, name, description and provides
 * a suspend handler function that processes tool calls.
 */
interface ToolHandler {
    /** The name of the tool (e.g., "scopes.get") */
    val name: String

    /** Human-readable description of what this tool does */
    val description: String

    /** Input schema definition for this tool */
    val input: Tool.Input

    /** Optional output schema definition for this tool */
    val output: Tool.Output?

    /**
     * Handle a tool call with the given context.
     *
     * @param ctx The tool execution context containing arguments, ports, and services
     * @return The result of the tool execution
     */
    suspend fun handle(ctx: ToolContext): CallToolResult
}

/**
 * Context provided to tool handlers containing all necessary dependencies.
 */
data class ToolContext(
    /** Raw arguments passed to the tool */
    val args: Map<String, JsonElement>,
    /** Access to domain ports */
    val ports: Ports,
    /** Access to support services */
    val services: Services,
)

/**
 * Container for domain ports used by tools.
 */
data class Ports(val query: ScopeManagementQueryPort, val command: ScopeManagementCommandPort)

/**
 * Container for support services used by tools.
 */
data class Services(val errors: ErrorMapper, val idempotency: IdempotencyService, val codec: ArgumentCodec, val logger: Logger)
