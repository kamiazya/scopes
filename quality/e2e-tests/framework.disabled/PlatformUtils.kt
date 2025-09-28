package io.github.kamiazya.scopes.e2e.framework

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Platform-specific utilities for E2E testing.
 */
object PlatformUtils {
    
    data class Platform(
        val os: OS,
        val arch: Architecture,
    ) {
        fun toBinaryIdentifier(): String = "${os.identifier}-${arch.identifier}"
        
        fun isWindows(): Boolean = os == OS.WINDOWS
        fun isMacOS(): Boolean = os == OS.MACOS
        fun isLinux(): Boolean = os == OS.LINUX
        fun isUnix(): Boolean = isMacOS() || isLinux()
    }
    
    enum class OS(val identifier: String) {
        WINDOWS("win32"),
        MACOS("darwin"),
        LINUX("linux"),
        UNKNOWN("unknown");
        
        companion object {
            fun detect(): OS {
                val osName = System.getProperty("os.name").lowercase()
                return when {
                    osName.contains("win") -> WINDOWS
                    osName.contains("mac") -> MACOS
                    osName.contains("linux") -> LINUX
                    else -> UNKNOWN
                }
            }
        }
    }
    
    enum class Architecture(val identifier: String) {
        X64("x64"),
        ARM64("arm64"),
        UNKNOWN("unknown");
        
        companion object {
            fun detect(): Architecture {
                val osArch = System.getProperty("os.arch").lowercase()
                return when {
                    osArch.contains("amd64") || osArch.contains("x86_64") -> X64
                    osArch.contains("aarch64") || osArch.contains("arm64") -> ARM64
                    else -> UNKNOWN
                }
            }
        }
    }
    
    val currentPlatform: Platform by lazy {
        Platform(OS.detect(), Architecture.detect())
    }
    
    fun getExecutableName(baseName: String): String {
        return if (currentPlatform.isWindows()) "$baseName.exe" else baseName
    }
    
    fun makeExecutable(file: File) {
        if (currentPlatform.isUnix()) {
            file.setExecutable(true)
        }
    }
    
    fun getEndpointFilePath(): Path {
        return when (currentPlatform.os) {
            OS.LINUX -> {
                val runtimeDir = System.getenv("XDG_RUNTIME_DIR")
                if (runtimeDir != null) {
                    Paths.get(runtimeDir, "scopes", "scopesd.endpoint")
                } else {
                    Paths.get(System.getProperty("user.home"), ".scopes", "run", "scopesd.endpoint")
                }
            }
            OS.MACOS -> {
                Paths.get(
                    System.getProperty("user.home"),
                    "Library", "Application Support", "scopes", "run", "scopesd.endpoint"
                )
            }
            OS.WINDOWS -> {
                val localAppData = System.getenv("LOCALAPPDATA")
                    ?: System.getenv("APPDATA")
                    ?: Paths.get(System.getProperty("user.home"), "AppData", "Local").toString()
                Paths.get(localAppData, "scopes", "run", "scopesd.endpoint")
            }
            else -> throw UnsupportedOperationException("Unsupported platform: ${currentPlatform.os}")
        }
    }
    
    fun getConfigDir(): Path {
        return when (currentPlatform.os) {
            OS.LINUX -> {
                val configHome = System.getenv("XDG_CONFIG_HOME")
                if (configHome != null) {
                    Paths.get(configHome, "scopes")
                } else {
                    Paths.get(System.getProperty("user.home"), ".config", "scopes")
                }
            }
            OS.MACOS -> {
                Paths.get(
                    System.getProperty("user.home"),
                    "Library", "Application Support", "scopes"
                )
            }
            OS.WINDOWS -> {
                val appData = System.getenv("APPDATA")
                    ?: Paths.get(System.getProperty("user.home"), "AppData", "Roaming").toString()
                Paths.get(appData, "scopes")
            }
            else -> throw UnsupportedOperationException("Unsupported platform: ${currentPlatform.os}")
        }
    }
    
    fun killProcess(pid: Long) {
        if (currentPlatform.isWindows()) {
            ProcessBuilder("taskkill", "/F", "/PID", pid.toString())
                .start()
                .waitFor()
        } else {
            ProcessBuilder("kill", "-9", pid.toString())
                .start()
                .waitFor()
        }
    }
    
    fun checkPortAvailable(port: Int): Boolean {
        return try {
            java.net.ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    fun findAvailablePort(startPort: Int = 50000, endPort: Int = 60000): Int {
        for (port in startPort..endPort) {
            if (checkPortAvailable(port)) {
                return port
            }
        }
        throw IllegalStateException("No available ports found in range $startPort-$endPort")
    }
}