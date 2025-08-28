package io.github.kamiazya.scopes.scopemanagement.eventstore.model

import io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject.DeviceId
import kotlinx.serialization.Serializable

/**
 * Vector clock implementation for tracking causality between events across devices.
 *
 * A vector clock is a map from device IDs to logical timestamps that allows us to
 * determine the causal ordering of events in a distributed system.
 */
@Serializable
data class VectorClock(val clocks: Map<String, Long> = emptyMap()) {
    /**
     * Increments the clock for a specific device.
     */
    fun increment(deviceId: DeviceId): VectorClock {
        val currentValue = clocks[deviceId.value] ?: 0L
        return copy(clocks = clocks + (deviceId.value to currentValue + 1))
    }

    /**
     * Merges this vector clock with another, taking the maximum value for each device.
     */
    fun merge(other: VectorClock): VectorClock {
        val allDevices = (clocks.keys + other.clocks.keys).toSet()
        val mergedClocks = allDevices.associateWith { deviceId ->
            maxOf(clocks[deviceId] ?: 0L, other.clocks[deviceId] ?: 0L)
        }
        return VectorClock(mergedClocks)
    }

    /**
     * Checks if this vector clock happened before another.
     * Returns true if this clock is strictly less than the other for all devices.
     */
    fun happenedBefore(other: VectorClock): Boolean {
        val allDevices = (clocks.keys + other.clocks.keys).toSet()
        var isStrictlyLess = false

        for (deviceId in allDevices) {
            val thisValue = clocks[deviceId] ?: 0L
            val otherValue = other.clocks[deviceId] ?: 0L

            if (thisValue > otherValue) {
                return false
            }
            if (thisValue < otherValue) {
                isStrictlyLess = true
            }
        }

        return isStrictlyLess
    }

    /**
     * Checks if this vector clock is concurrent with another.
     * Two clocks are concurrent if neither happened before the other and they are not identical.
     */
    fun isConcurrentWith(other: VectorClock): Boolean = this.clocks != other.clocks && !this.happenedBefore(other) && !other.happenedBefore(this)

    /**
     * Gets the logical timestamp for a specific device.
     */
    fun getTimestamp(deviceId: DeviceId): Long = clocks[deviceId.value] ?: 0L

    companion object {
        /**
         * Creates an empty vector clock.
         */
        fun empty(): VectorClock = VectorClock()

        /**
         * Creates a vector clock with a single device initialized to a specific value.
         */
        fun single(deviceId: DeviceId, value: Long = 1L): VectorClock = VectorClock(mapOf(deviceId.value to value))
    }
}
