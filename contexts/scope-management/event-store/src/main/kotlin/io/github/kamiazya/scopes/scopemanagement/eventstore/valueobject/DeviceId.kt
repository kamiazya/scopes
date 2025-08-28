@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject

import kotlin.uuid.Uuid

/**
 * Represents a unique identifier for a device in the multi-device synchronization system.
 *
 * Each device participating in synchronization has a unique ID that is used to track
 * which events originated from which device and to maintain vector clocks.
 */
@JvmInline
value class DeviceId(val value: String) {
    init {
        require(value.isNotBlank()) { "Device ID cannot be blank" }
        require(value.length <= 64) { "Device ID cannot exceed 64 characters" }
        require(value.matches(Regex("^[a-zA-Z0-9-_]+$"))) {
            "Device ID can only contain alphanumeric characters, hyphens, and underscores"
        }
    }

    companion object {
        /**
         * Generates a new random device ID.
         */
        fun generate(): DeviceId = DeviceId(Uuid.random().toString())

        /**
         * Creates a device ID from a string, returning null if invalid.
         */
        fun fromStringOrNull(value: String): DeviceId? = try {
            DeviceId(value)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
