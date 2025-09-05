package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand
import kotlinx.coroutines.runBlocking

/**
 * Main entry point for the Scopes CLI application.
 *
 * This is a minimal launcher that initializes the DI container
 * and delegates to the ScopesCommand from the interfaces layer.
 */
fun main(args: Array<String>) {
    // Set log directory based on OS standards if not already set
    if (System.getenv("LOG_DIR") == null) {
        val logDir = getStandardLogDirectory()
        System.setProperty("LOG_DIR", logDir)
    }

    // Use native-specific config if running in native image
    if (System.getProperty("org.graalvm.nativeimage.kind") != null) {
        System.setProperty("logback.configurationFile", "logback-native.xml")
    }

    ScopesCliApplication().use {
        // Initialize aspect presets before running commands
        runBlocking { it.ensureInitialized() }
        it.container.get<ScopesCommand>().main(args)
    }
}

/**
 * Gets the standard log directory based on the operating system.
 */
private fun getStandardLogDirectory(): String {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")

    return when {
        os.contains("linux") -> {
            // XDG Base Directory specification
            val dataHome = System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
            "$dataHome/scopes/logs"
        }
        os.contains("mac") || os.contains("darwin") -> {
            // macOS logs in Application Support
            "$home/Library/Application Support/scopes/logs"
        }
        os.contains("windows") -> {
            // Windows logs in AppData/Local
            val localAppData = System.getenv("LOCALAPPDATA") ?: "$home/AppData/Local"
            "$localAppData/scopes/logs"
        }
        else -> {
            // Fallback to XDG-style directory
            "$home/.local/share/scopes/logs"
        }
    }
}
