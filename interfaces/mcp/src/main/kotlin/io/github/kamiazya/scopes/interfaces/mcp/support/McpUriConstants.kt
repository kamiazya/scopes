package io.github.kamiazya.scopes.interfaces.mcp.support

/**
 * Constants for MCP resource URI patterns to avoid magic strings
 * and ensure consistency across resource handlers.
 */
object McpUriConstants {
    /**
     * URI prefix for scope detail resources.
     * Example: scopes:/scope/my-alias
     */
    const val SCOPE_PREFIX = "scopes:/scope/"

    /**
     * URI prefix for tree JSON resources.
     * Example: scopes:/tree/my-alias?depth=3
     */
    const val TREE_JSON_PREFIX = "scopes:/tree/"

    /**
     * URI prefix for tree Markdown resources.
     * Example: scopes:/tree.md/my-alias
     */
    const val TREE_MARKDOWN_PREFIX = "scopes:/tree.md/"
}
