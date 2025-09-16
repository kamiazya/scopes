package io.github.kamiazya.scopes.interfaces.mcp.prompts.providers

import io.github.kamiazya.scopes.interfaces.mcp.prompts.PromptProvider
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.TextContent

/**
 * Prompt provider for creating scope outlines.
 *
 * This provider generates prompts for creating structured bullet-point outlines of scopes.
 */
class ScopesOutlinePromptProvider : PromptProvider {

    override val name: String = "prompts.scopes.outline"

    override val description: String = "Create a concise bullet-point outline for a scope"

    override val arguments: List<PromptArgument> = listOf(
        PromptArgument(name = "alias", description = "Scope alias", required = true),
        PromptArgument(name = "depth", description = "Outline depth (1-3)", required = false),
    )

    override suspend fun get(args: Map<String, String?>): GetPromptResult {
        val alias = args["alias"] ?: ""
        val depth = args["depth"] ?: "2"

        return GetPromptResult(
            description = "Outline scope",
            messages = listOf(
                PromptMessage(
                    role = Role.user,
                    content = TextContent("You are a helpful assistant that produces crisp, structured outlines."),
                ),
                PromptMessage(
                    role = Role.user,
                    content = TextContent(
                        "Create a bullet-point outline (depth=$depth) for scope <alias>$alias</alias>.\n" +
                            "- Use short, informative bullets.\n" +
                            "- Group by sub-areas.\n" +
                            "- Do not invent facts beyond scope description and children.",
                    ),
                ),
            ),
        )
    }
}
