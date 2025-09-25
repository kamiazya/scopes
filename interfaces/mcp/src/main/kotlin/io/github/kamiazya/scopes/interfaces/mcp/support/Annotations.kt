package io.github.kamiazya.scopes.interfaces.mcp.support

import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations

/**
 * Helper functions to create common ToolAnnotations patterns
 * to reduce repetition across tool handlers.
 */
object Annotations {
    fun readOnlyIdempotent(): ToolAnnotations = ToolAnnotations(
        title = null,
        readOnlyHint = true,
        destructiveHint = false,
        idempotentHint = true,
    )

    fun destructiveNonIdempotent(): ToolAnnotations = ToolAnnotations(
        title = null,
        readOnlyHint = false,
        destructiveHint = true,
        idempotentHint = false,
    )
}
