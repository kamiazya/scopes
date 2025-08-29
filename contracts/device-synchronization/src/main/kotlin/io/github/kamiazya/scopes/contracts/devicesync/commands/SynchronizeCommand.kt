package io.github.kamiazya.scopes.contracts.devicesync.commands

/**
 * Command to synchronize events between devices.
 */
public data class SynchronizeCommand(
    val localDeviceId: String,
    val remoteDeviceId: String,
    val conflictResolutionStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LATEST_TIMESTAMP,
)

/**
 * Strategies for resolving conflicts between events.
 */
public enum class ConflictResolutionStrategy {
    /** Keep the local version */
    KEEP_LOCAL,

    /** Keep the remote version */
    KEEP_REMOTE,

    /** Keep the version with the latest timestamp */
    LATEST_TIMESTAMP,

    /** Mark for manual review */
    MANUAL_REVIEW,
}
