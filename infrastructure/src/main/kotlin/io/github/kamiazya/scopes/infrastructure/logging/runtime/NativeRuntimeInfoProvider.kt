package io.github.kamiazya.scopes.infrastructure.logging.runtime

/**
 * Provider for native runtime information.
 * Gathers system and process information from the JVM/Native environment.
 */
object NativeRuntimeInfoProvider {

    /**
     * Creates a NativeRuntimeInfo instance with current system information.
     */
    fun get(): NativeRuntimeInfo {
        val runtime = Runtime.getRuntime()

        return NativeRuntimeInfo(
            processId = ProcessHandle.current().pid(),
            hostname = getHostname(),
            osName = System.getProperty("os.name", "Unknown"),
            osVersion = System.getProperty("os.version", "Unknown"),
            architecture = System.getProperty("os.arch", "Unknown"),
            availableProcessors = runtime.availableProcessors(),
            totalMemory = runtime.totalMemory(),
            maxMemory = runtime.maxMemory(),
        )
    }

    /**
     * Gets the hostname from environment variable or system property,
     * with fallback to "localhost" if unable to determine.
     */
    private fun getHostname(): String = System.getenv("HOSTNAME")
        ?: System.getenv("COMPUTERNAME") // Windows
        ?: try {
            // Try to get from system property
            System.getProperty("user.name") + "-host"
        } catch (e: Exception) {
            "localhost"
        }
}
