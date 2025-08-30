package io.github.kamiazya.scopes.interfaces.cli.commands

/**
 * Context object to pass debug mode information to subcommands.
 *
 * This class is used to share the global --debug flag state with
 * all subcommands through the Clikt command context.
 */
data class DebugContext(val isDebugEnabled: Boolean) {
    /**
     * Short alias for isDebugEnabled for convenient access
     */
    val debug: Boolean get() = isDebugEnabled
}
