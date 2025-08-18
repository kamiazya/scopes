package io.github.kamiazya.scopes.infrastructure.logging.runtime

import io.github.kamiazya.scopes.application.logging.RuntimeInfo

/**
 * Runtime information for native/JVM environments.
 * Captures OS, process, and system-level details.
 */
data class NativeRuntimeInfo(
    val processId: Long,
    val hostname: String,
    val osName: String,
    val osVersion: String,
    val architecture: String,
    val availableProcessors: Int,
    val totalMemory: Long,
    val maxMemory: Long
) : RuntimeInfo {

    override fun toMap(): Map<String, Any> {
        return mapOf(
            "runtime.type" to "native",
            "process.id" to processId,
            "host.name" to hostname,
            "os.name" to osName,
            "os.version" to osVersion,
            "os.arch" to architecture,
            "cpu.count" to availableProcessors,
            "memory.total" to totalMemory,
            "memory.max" to maxMemory
        )
    }
}
