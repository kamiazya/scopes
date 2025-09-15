package io.github.kamiazya.scopes.interfaces.mcp.support

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.modelcontextprotocol.kotlin.sdk.CallToolResult

/**
 * Service for mapping domain contract errors to MCP tool results.
 *
 * This centralizes error handling logic and ensures consistent
 * error responses across all tools.
 */
interface ErrorMapper {
    /**
     * Map a contract error to an MCP tool result.
     *
     * @param error The domain contract error
     * @return A CallToolResult representing the error
     */
    fun mapContractError(error: ScopeContractError): CallToolResult

    /**
     * Create a generic error result for exceptions.
     *
     * @param message The error message
     * @param code Optional error code
     * @return A CallToolResult representing the error
     */
    fun errorResult(message: String, code: Int? = null): CallToolResult
    
    /**
     * Create a successful result with JSON content.
     * 
     * @param content The success content as a string
     * @return A CallToolResult representing success
     */
    fun successResult(content: String): CallToolResult
}
