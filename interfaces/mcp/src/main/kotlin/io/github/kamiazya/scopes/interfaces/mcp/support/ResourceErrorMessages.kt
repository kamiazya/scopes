package io.github.kamiazya.scopes.interfaces.mcp.support

/**
 * Centralized resource error messages to keep wording consistent
 * across resource handlers without changing existing semantics.
 */
object ResourceErrorMessages {
    const val MISSING_ALIAS_JSON = "Missing or invalid alias in resource URI"
    const val MISSING_ALIAS_TREE_JSON = "Missing or invalid alias in resource URI. Optional ?depth=1..5 supported."
    const val MISSING_ALIAS_TEXT = "Invalid resource: missing alias"
}
