package io.github.kamiazya.scopes.scopemanagement.application.query.response.builders

/**
 * Base interface for building responses in different formats from query result data.
 *
 * This interface defines the contract for transforming domain query results
 * into format-specific representations suitable for different client interfaces.
 *
 * @param T The type of the query response data to be formatted
 */
interface ResponseBuilder<T> {
    /**
     * Builds a response suitable for MCP (Model Context Protocol) interfaces.
     *
     * @param data The query response data to format
     * @return A map representation suitable for JSON serialization in MCP contexts
     */
    fun buildMcpResponse(data: T): Map<String, Any>

    /**
     * Builds a response suitable for CLI (Command Line Interface) output.
     *
     * @param data The query response data to format
     * @return A human-readable string representation for terminal display
     */
    fun buildCliResponse(data: T): String
}
