package io.github.kamiazya.scopes.interfaces.mcp.prompts

import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.PromptArgument

/**
 * Interface for providing MCP prompt implementations.
 *
 * Each prompt provider is responsible for a specific prompt template and handles
 * argument processing and message generation.
 */
interface PromptProvider {

    /**
     * Name of the prompt (e.g., "prompts.scopes.summarize")
     */
    val name: String

    /**
     * Description of what this prompt does
     */
    val description: String

    /**
     * List of arguments this prompt accepts
     */
    val arguments: List<PromptArgument>

    /**
     * Generate the prompt result from the provided arguments.
     *
     * @param args Map of argument names to values
     * @return The prompt result with generated messages
     */
    suspend fun get(args: Map<String, String?>): GetPromptResult
}
