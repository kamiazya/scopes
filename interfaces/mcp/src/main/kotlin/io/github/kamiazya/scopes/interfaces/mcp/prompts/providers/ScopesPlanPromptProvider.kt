package io.github.kamiazya.scopes.interfaces.mcp.prompts.providers

import io.github.kamiazya.scopes.interfaces.mcp.prompts.PromptProvider
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.TextContent

/**
 * Prompt provider for planning scope next steps.
 * 
 * This provider generates prompts for proposing practical next steps for scopes.
 */
class ScopesPlanPromptProvider : PromptProvider {
    
    override val name: String = "prompts.scopes.plan"
    
    override val description: String = "Propose practical next steps for a scope"
    
    override val arguments: List<PromptArgument> = listOf(
        PromptArgument(name = "alias", description = "Scope alias", required = true),
        PromptArgument(name = "timeHorizon", description = "e.g. '1 week' or '1 month'", required = false)
    )
    
    override suspend fun get(args: Map<String, String?>): GetPromptResult {
        val alias = args["alias"] ?: ""
        val horizon = args["timeHorizon"] ?: "2 weeks"
        
        return GetPromptResult(
            description = "Plan next steps",
            messages = listOf(
                PromptMessage(
                    role = Role.user,
                    content = TextContent("You write actionable plans that balance impact and effort.")
                ),
                PromptMessage(
                    role = Role.user,
                    content = TextContent(
                        "Given scope <alias>$alias</alias>, propose prioritized next steps for the next $horizon.\n" +
                        "- Each step should have an outcome and owner suggestion.\n" +
                        "- Keep it concise and concrete."
                    )
                )
            )
        )
    }
}