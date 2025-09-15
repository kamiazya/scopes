package io.github.kamiazya.scopes.interfaces.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered

/**
 * Factory for creating MCP server transports.
 * 
 * This encapsulates transport creation logic and provides different transport types
 * for production and testing scenarios.
 */
class TransportFactory {
    
    /**
     * Create a stdio-based server transport.
     * 
     * @param source The input source for reading MCP messages
     * @param sink The output sink for writing MCP responses
     * @return A configured stdio transport
     */
    fun stdio(source: Source, sink: Sink): StdioServerTransport {
        return StdioServerTransport(
            inputStream = source.buffered(),
            outputStream = sink.buffered()
        )
    }
    
    companion object {
        /**
         * Create a transport factory instance.
         */
        fun create(): TransportFactory = TransportFactory()
    }
}