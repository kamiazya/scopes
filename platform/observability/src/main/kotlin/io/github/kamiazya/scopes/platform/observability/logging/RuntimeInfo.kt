package io.github.kamiazya.scopes.platform.observability.logging

/**
 * Interface representing runtime environment information.
 * Implementations can provide environment-specific details (JVM, Browser, Native, etc.)
 */
interface RuntimeInfo {
    /**
     * Converts the runtime information to a map of key-value pairs.
     * Keys should be descriptive and values should be serializable.
     */
    fun toMap(): Map<String, Any>

    /**
     * Converts the runtime information to a map with LogValue for type-safe serialization.
     */
    fun toLogValueMap(): Map<String, LogValue> = toMap().toLogValueMap()
}
