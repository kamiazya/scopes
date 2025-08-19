package io.github.kamiazya.scopes.application.logging

/**
 * Represents the type of application that is generating logs.
 * Used to distinguish between different Scopes components in a multi-process environment.
 */
enum class ApplicationType {
    /**
     * Command Line Interface application
     */
    CLI,

    /**
     * Model Context Protocol server
     */
    MCP_SERVER,

    /**
     * System-level processes (e.g., CRON jobs, background tasks)
     */
    SYSTEM,

    /**
     * Central management daemon
     */
    DAEMON,

    /**
     * Browser-based client application
     */
    BROWSER_CLIENT,

    /**
     * Plugin or extension
     */
    PLUGIN,

    /**
     * Custom application type
     */
    CUSTOM
}

