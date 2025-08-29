package io.github.kamiazya.scopes.devicesync.application.handler

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.devicesync.application.command.SynchronizeDevice
import io.github.kamiazya.scopes.devicesync.application.dto.SyncStatusDto
import io.github.kamiazya.scopes.devicesync.application.dto.SynchronizationResultDto
import io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError
import io.github.kamiazya.scopes.devicesync.domain.service.DeviceSynchronizationService
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.application.usecase.UseCase

/**
 * Handler for synchronizing with a remote device.
 */
class SynchronizeDeviceHandler(private val synchronizationService: DeviceSynchronizationService, private val transactionManager: TransactionManager) :
    UseCase<SynchronizeDevice, DeviceSyncApplicationError, SynchronizationResultDto> {

    override suspend fun invoke(input: SynchronizeDevice): Either<DeviceSyncApplicationError, SynchronizationResultDto> {
        // Validate device ID
        val deviceId = DeviceId.fromStringOrNull(input.remoteDeviceId)
            ?: return Either.Left(
                DeviceSyncApplicationError.ValidationError(
                    fieldName = "remoteDeviceId",
                    invalidValue = input.remoteDeviceId,
                    validationRule = DeviceSyncApplicationError.ValidationRule.FORMAT_INVALID,
                ),
            )

        return transactionManager.inTransaction {
            synchronizationService.synchronize(
                remoteDeviceId = deviceId,
                since = input.since,
            )
                .mapLeft { error ->
                    DeviceSyncApplicationError.SyncOperationError(
                        operation = DeviceSyncApplicationError.SyncOperation.FULL_SYNC,
                        deviceId = input.remoteDeviceId,
                        failureReason = when (error) {
                            is io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError.NetworkError ->
                                DeviceSyncApplicationError.SyncFailureReason.NETWORK_ERROR
                            is io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError.InvalidDeviceError ->
                                DeviceSyncApplicationError.SyncFailureReason.AUTHENTICATION_FAILED
                            is io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError.ConflictResolutionError ->
                                DeviceSyncApplicationError.SyncFailureReason.DATA_CORRUPTION
                            else -> null
                        },
                        occurredAt = error.occurredAt,
                        cause = null,
                    )
                }
                .flatMap { result ->
                    // Handle conflicts if any
                    if (result.conflicts.isNotEmpty()) {
                        synchronizationService.resolveConflicts(
                            conflicts = result.conflicts,
                            strategy = input.conflictStrategy,
                        )
                            .mapLeft { error ->
                                DeviceSyncApplicationError.SyncOperationError(
                                    operation = DeviceSyncApplicationError.SyncOperation.CONFLICT_RESOLUTION,
                                    deviceId = input.remoteDeviceId,
                                    failureReason = DeviceSyncApplicationError.SyncFailureReason.DATA_CORRUPTION,
                                    occurredAt = error.occurredAt,
                                    cause = null,
                                )
                            }
                            .map { resolution ->
                                val status = when {
                                    resolution.unresolved.isEmpty() -> SyncStatusDto.SUCCESS
                                    resolution.resolved.isEmpty() -> SyncStatusDto.CONFLICTS_PENDING
                                    else -> SyncStatusDto.PARTIAL_SUCCESS
                                }

                                SynchronizationResultDto(
                                    deviceId = input.remoteDeviceId,
                                    eventsPushed = result.eventsPushed,
                                    eventsPulled = result.eventsPulled,
                                    conflictsDetected = result.conflicts.size,
                                    conflictsResolved = resolution.resolved.size,
                                    syncedAt = result.syncedAt,
                                    status = status,
                                )
                            }
                    } else {
                        Either.Right(
                            SynchronizationResultDto(
                                deviceId = input.remoteDeviceId,
                                eventsPushed = result.eventsPushed,
                                eventsPulled = result.eventsPulled,
                                conflictsDetected = 0,
                                conflictsResolved = 0,
                                syncedAt = result.syncedAt,
                                status = SyncStatusDto.SUCCESS,
                            ),
                        )
                    }
                }
        }
    }
}
