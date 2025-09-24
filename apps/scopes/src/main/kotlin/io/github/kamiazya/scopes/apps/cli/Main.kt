package io.github.kamiazya.scopes.apps.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.main
import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand
import io.github.kamiazya.scopes.interfaces.cli.exitcode.ExitCode
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * Main entry point for the Scopes CLI application.
 *
 * This is a minimal launcher that initializes the DI container
 * and delegates to the ScopesCommand from the interfaces layer.
 * Handles exit codes according to UNIX conventions.
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

    try {
        ScopesCliApplication().use {
            // Initialize application lifecycle before running commands
            runBlocking { it.initialize() }
            it.container.get<ScopesCommand>().main(args)
        }
        // Successful execution
        exitProcess(ExitCode.SUCCESS.code)
    } catch (e: CliktError) {
        // Check if a specific exit code was set
        val exitCode = System.getProperty("scopes.cli.exit.code")?.toIntOrNull() ?: ExitCode.GENERAL_ERROR.code
        System.clearProperty("scopes.cli.exit.code") // Clean up
        exitProcess(exitCode)
    } catch (e: Exception) {
        // Unexpected error - log details for debugging without exposing stack trace to users
        System.err.println("Fatal error: ${e.message}")
        System.err.println("Error type: ${e.javaClass.simpleName}")
        exitProcess(ExitCode.SOFTWARE_ERROR.code)
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
