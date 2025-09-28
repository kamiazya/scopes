package io.github.kamiazya.scopes.platform.infrastructure.endpoint

import java.io.File

/**
 * Utility functions for daemon endpoint file management.
 * Provides platform-specific endpoint file path resolution used by both CLI and daemon.
 */
object EndpointFileUtils {

    /**
     * Gets the default endpoint file path based on the current platform.
     *
     * The file path follows platform conventions:
     * - macOS: ~/Library/Application Support/scopes/run/scopesd.endpoint
     * - Windows: %LOCALAPPDATA%/scopes/run/scopesd.endpoint (or %APPDATA% as fallback)
     * - Linux/Unix: $XDG_RUNTIME_DIR/scopes/scopesd.endpoint (or ~/.local/share/scopes/run/scopesd.endpoint as fallback)
     *
     * @return File object representing the endpoint file path
     */
    fun getDefaultEndpointFile(): File {
        val osName = System.getProperty("os.name")?.lowercase() ?: ""

        return when {
            osName.contains("mac") || osName.contains("darwin") -> {
                val userHome = System.getProperty("user.home")
                File("$userHome/Library/Application Support/scopes/run/scopesd.endpoint")
            }
            osName.contains("windows") -> {
                val localAppData = System.getenv("LOCALAPPDATA")
                    ?: System.getenv("APPDATA")
                    ?: System.getProperty("user.home")
                File("$localAppData/scopes/run/scopesd.endpoint")
            }
            else -> {
                // Linux and other Unix-like systems
                val xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR")
                if (!xdgRuntimeDir.isNullOrBlank()) {
                    File("$xdgRuntimeDir/scopes/scopesd.endpoint")
                } else {
                    val userHome = System.getProperty("user.home")
                    File("$userHome/.local/share/scopes/run/scopesd.endpoint")
                }
            }
        }
    }

    /**
     * Ensures the parent directory of the endpoint file exists.
     * Creates the directory structure if it doesn't exist.
     *
     * @param endpointFile The endpoint file whose parent directory should be created
     * @return true if the directory exists or was created successfully, false otherwise
     */
    fun ensureEndpointDirectoryExists(endpointFile: File): Boolean {
        val parentDir = endpointFile.parentFile ?: return false
        return if (!parentDir.exists()) {
            parentDir.mkdirs()
        } else {
            true
        }
    }

    /**
     * Sets appropriate permissions on the endpoint file for security.
     * On Unix-like systems, this sets the file to be readable/writable by owner only (0600).
     * On Windows, this is a no-op as Windows handles file permissions differently.
     *
     * @param endpointFile The endpoint file to set permissions on
     * @return true if permissions were set successfully or are not applicable, false on error
     */
    fun setSecurePermissions(endpointFile: File): Boolean {
        val osName = System.getProperty("os.name")?.lowercase() ?: ""

        return if (osName.contains("windows")) {
            // Windows file permissions are handled differently
            // The endpoint file should be in a user-specific directory which provides security
            true
        } else {
            try {
                // Unix-like systems: Set to owner read/write only (0600)
                endpointFile.setReadable(false, false) // Remove read for others
                endpointFile.setReadable(true, true) // Add read for owner
                endpointFile.setWritable(false, false) // Remove write for others
                endpointFile.setWritable(true, true) // Add write for owner
                endpointFile.setExecutable(false, false) // Remove execute for all
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
