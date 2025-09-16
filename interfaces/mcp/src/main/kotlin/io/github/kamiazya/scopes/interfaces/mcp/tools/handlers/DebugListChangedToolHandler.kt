package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * Tool handler for triggering list_changed notifications (debug only).
 *
 * This is a development-only helper to trigger list_changed notifications for tools/resources/prompts.
 * Currently it's a no-op that simply returns success.
 */
class DebugListChangedToolHandler : ToolHandler {

    override val name: String = "debug.listChanged"

    override val description: String = "Trigger list_changed notifications for tools/resources/prompts (debug only, currently no-op)"

    override val input: Tool.Input = Tool.Input(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") { add("target") }
            putJsonObject("properties") {
                putJsonObject("target") {
                    put("type", "string")
                    putJsonArray("enum") {
                        add("tools")
                        add("resources")
                        add("prompts")
                    }
                }
            }
        },
    )

    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("ok") { put("type", "boolean") }
                putJsonObject("target") { put("type", "string") }
            }
            putJsonArray("required") {
                add("ok")
                add("target")
            }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val target = ctx.services.codec.getString(ctx.args, "target", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'target' parameter")

        ctx.services.logger.debug("Debug list changed notification for: $target")

        // TODO: In the future, this could trigger actual list_changed notifications
        // For now, it's just a no-op that returns success
        val ok = when (target) {
            "tools", "resources", "prompts" -> true
            else -> false
        }

        val json = buildJsonObject {
            put("ok", ok)
            put("target", target)
        }

        return ctx.services.errors.successResult(json.toString())
    }
}
