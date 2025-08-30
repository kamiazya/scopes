package io.github.kamiazya.scopes.apps.cli.di.platform

import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString

/**
 * Koin module for database infrastructure.
 *
 * This module provides:
 * - Database path configuration
 * - Transaction manager
 */
val databaseModule = module {
    // Database Path
    single<String>(named("databasePath")) {
        val environment = getProperty<String>("app.environment", "production")
        val path = when (environment) {
            "test" -> ":memory:"
            else -> {
                // Use XDG Base Directory specification or OS-specific standard directories
                val dataDir = getStandardDataDirectory()
                val scopesDir = when (environment) {
                    "development" -> Path(dataDir, "scopes-dev")
                    else -> Path(dataDir, "scopes")
                }
                scopesDir.pathString
            }
        }
        if (path != ":memory:") {
            ensureDirectoryExists(path)
        }
        path
    }

    // Note: Each bounded context will provide its own TransactionManager
    // that is specific to its database
}

/**
 * Gets the standard data directory based on the operating system.
 *
 * Follows:
 * - XDG Base Directory specification on Linux
 * - Application Support on macOS
 * - AppData/Local on Windows
 */
private fun getStandardDataDirectory(): String {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")

    return when {
        os.contains("linux") -> {
            // XDG Base Directory specification
            System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
        }
        os.contains("mac") || os.contains("darwin") -> {
            // macOS Application Support
            "$home/Library/Application Support"
        }
        os.contains("windows") -> {
            // Windows AppData/Local
            System.getenv("LOCALAPPDATA") ?: "$home/AppData/Local"
        }
        else -> {
            // Fallback to XDG-style directory
            "$home/.local/share"
        }
    }
}

/**
 * Ensures the directory for the database files exists.
 */
private fun ensureDirectoryExists(databasePath: String) {
    val path = Path(databasePath)
    path.createDirectories()
}
