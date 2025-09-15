package io.github.kamiazya.scopes.interfaces.mcp.resources

import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult

/**
 * Interface for handling MCP resource requests.
 * 
 * Each resource handler is responsible for a specific URI pattern and provides
 * read functionality for that resource type.
 */
interface ResourceHandler {
    
    /**
     * URI pattern for this resource (e.g., "scopes:/scope/{canonicalAlias}")
     */
    val uriPattern: String
    
    /**
     * Display name for this resource
     */
    val name: String
    
    /**
     * Description of what this resource provides
     */
    val description: String
    
    /**
     * MIME type of the resource content
     */
    val mimeType: String
    
    /**
     * Handle a read request for this resource.
     * 
     * @param req The resource request containing the URI
     * @param ports Access to domain operations
     * @param services Support services for error handling, etc.
     * @return The resource result with content and metadata
     */
    suspend fun read(req: ReadResourceRequest, ports: Ports, services: Services): ReadResourceResult
}