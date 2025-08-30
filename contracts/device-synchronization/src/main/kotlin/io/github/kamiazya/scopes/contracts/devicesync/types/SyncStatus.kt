package io.github.kamiazya.scopes.contracts.devicesync.types

/**
 * Synchronization status for the contract layer.
 */
public enum class SyncStatus {
    /**
     * Not synchronized yet.
     */
    NOT_SYNCED,

    /**
     * Currently synchronizing.
     */
    IN_PROGRESS,

    /**
     * Successfully synchronized.
     */
    SYNCED,

    /**
     * Synchronization failed.
     */
    FAILED,

    /**
     * Has conflicts that need resolution.
     */
    CONFLICT,
}
