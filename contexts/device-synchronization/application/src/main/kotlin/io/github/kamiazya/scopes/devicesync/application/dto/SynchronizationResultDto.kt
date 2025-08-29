package io.github.kamiazya.scopes.devicesync.application.dto

import kotlinx.datetime.Instant

/**
 * DTO representation of a synchronization result.
 */
data class SynchronizationResultDto(
    val deviceId: String,
    val eventsPushed: Int,
    val eventsPulled: Int,
    val conflictsDetected: Int,
    val conflictsResolved: Int,
    val syncedAt: Instant,
    val status: SyncStatusDto,
)

/**
 * Synchronization status.
 */
enum class SyncStatusDto {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    CONFLICTS_PENDING,
}
