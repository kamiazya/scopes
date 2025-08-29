package io.github.kamiazya.scopes.devicesync.application.command

import io.github.kamiazya.scopes.devicesync.domain.service.ConflictResolutionStrategy
import kotlinx.datetime.Instant

/**
 * Command to synchronize with a specific device.
 */
data class SynchronizeDevice(
    val remoteDeviceId: String,
    val since: Instant? = null,
    val conflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS,
)
