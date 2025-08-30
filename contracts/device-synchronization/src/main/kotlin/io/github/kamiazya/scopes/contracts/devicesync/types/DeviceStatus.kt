package io.github.kamiazya.scopes.contracts.devicesync.types

/**
 * Device status for the contract layer.
 */
public enum class DeviceStatus {
    /**
     * Device is registered and active.
     */
    ACTIVE,

    /**
     * Device is inactive (not currently in use).
     */
    INACTIVE,

    /**
     * Device has been revoked and cannot be used.
     */
    REVOKED,

    /**
     * Device is temporarily suspended.
     */
    SUSPENDED,
}
