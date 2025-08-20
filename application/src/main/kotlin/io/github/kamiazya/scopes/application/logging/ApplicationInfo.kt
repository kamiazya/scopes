package io.github.kamiazya.scopes.application.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

/**
 * Information about the application instance generating logs.
 * This includes version, type, and custom metadata specific to the application.
 */
data class ApplicationInfo(
    val name: String,
    val version: String,
    val type: ApplicationType,
    val startTime: Instant = Clock.System.now(),
    val instanceId: String = generateInstanceId(),
    val customMetadata: Map<String, Any> = emptyMap(),
) {
    /**
     * Converts the application information to a map of key-value pairs.
     */
    fun toMap(): Map<String, Any> = buildMap {
        put("app.name", name)
        put("app.version", version)
        put("app.type", type.name)
        put("app.start_time", startTime.toString())
        put("app.instance_id", instanceId)

        // Add custom metadata with "app.custom." prefix
        customMetadata.forEach { (key, value) ->
            put("app.custom.$key", value)
        }
    }

    /**
     * Converts the application information to a map with LogValue for type-safe serialization.
     */
    fun toLogValueMap(): Map<String, LogValue> = toMap().toLogValueMap()

    companion object {
        /**
         * Generates a unique instance ID for this application run.
         */
        private fun generateInstanceId(): String {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val random = Random.nextInt(10000, 99999)
            return "$timestamp-$random"
        }
    }
}
