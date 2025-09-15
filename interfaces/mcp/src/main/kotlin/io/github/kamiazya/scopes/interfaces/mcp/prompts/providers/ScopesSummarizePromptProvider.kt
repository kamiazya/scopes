package io.github.kamiazya.scopes.interfaces.mcp.prompts.providers

import io.github.kamiazya.scopes.interfaces.mcp.prompts.PromptProvider
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.TextContent

/**
 * Prompt provider for summarizing scopes.
 * 
 * This provider generates prompts for creating concise summaries of scopes by alias.
 */
class ScopesSummarizePromptProvider : PromptProvider {
    
    override val name: String = "prompts.scopes.summarize"
    
    override val description: String = "Summarize a scope by alias"
    
    override val arguments: List<PromptArgument> = listOf(
        PromptArgument(name = "alias", description = "Scope alias", required = true),
        PromptArgument(name = "level", description = "Summary level", required = false)
    )
    
    override suspend fun get(args: Map<String, String?>): GetPromptResult {
        val alias = args["alias"] ?: ""
        
        return GetPromptResult(
            description = "Summarize scope",
            messages = listOf(
                PromptMessage(
                    role = Role.user,
                    content = TextContent("Summarize the scope <alias>$alias</alias> concisely.")
                )
            )
        )
    }
}