package io.github.kamiazya.scopes.devicesync.application.adapter

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.devicesync.DeviceSynchronizationPort
import io.github.kamiazya.scopes.contracts.devicesync.commands.ConflictResolutionStrategy
import io.github.kamiazya.scopes.contracts.devicesync.commands.RegisterDeviceCommand
import io.github.kamiazya.scopes.contracts.devicesync.commands.SynchronizeCommand
import io.github.kamiazya.scopes.contracts.devicesync.errors.DeviceSynchronizationContractError
import io.github.kamiazya.scopes.contracts.devicesync.results.ConflictResolution
import io.github.kamiazya.scopes.contracts.devicesync.results.ConflictResult
import io.github.kamiazya.scopes.contracts.devicesync.results.RegisterDeviceResult
import io.github.kamiazya.scopes.contracts.devicesync.results.SynchronizationResult
import io.github.kamiazya.scopes.devicesync.application.command.SynchronizeDevice
import io.github.kamiazya.scopes.devicesync.application.handler.SynchronizeDeviceHandler
import io.github.kamiazya.scopes.devicesync.domain.repository.SynchronizationRepository
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import kotlinx.datetime.Clock
import io.github.kamiazya.scopes.devicesync.domain.service.ConflictResolutionStrategy as DomainConflictResolutionStrategy

/**
 * Adapter that implements the Device Sync contract port.
 */
class DeviceSyncPortAdapter(private val synchronizeHandler: SynchronizeDeviceHandler, private val syncRepository: SynchronizationRepository) :
    DeviceSynchronizationPort {

    override suspend fun registerDevice(command: RegisterDeviceCommand): Either<DeviceSynchronizationContractError, RegisterDeviceResult> {
        // Generate a new device ID
        val deviceId = DeviceId.generate()

        return syncRepository.registerDevice(deviceId)
            .mapLeft { error ->
                // Map domain errors to structured contract errors based on actual error type
                when (error) {
                    is io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError.InvalidDeviceError -> {
                        when (error.configurationIssue) {
                            io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError.ConfigurationIssue.INVALID_DEVICE_ID,
                            io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError.ConfigurationIssue.MISSING_DEVICE_ID,
                            ->
                                DeviceSynchronizationContractError.BusinessError.RegistrationFailed(
                                    failure = DeviceSynchronizationContractError.RegistrationFailureType.DuplicateDeviceId(
                                        deviceId = deviceId.value,
                                        registeredAt = Clock.System.now(),
                                    ),
                                )
                            else ->
                                DeviceSynchronizationContractError.BusinessError.RegistrationFailed(
                                    failure = DeviceSynchronizationContractError.RegistrationFailureType.InvalidDeviceName(
                                        deviceName = command.deviceName,
                                        reason = DeviceSynchronizationContractError.DeviceNameValidationFailure.InvalidCharacters(
                                            prohibitedCharacters = listOf(),
                                        ),
                                    ),
                                )
                        }
                    }
                    is io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError.NetworkError ->
                        DeviceSynchronizationContractError.SystemError.ServiceUnavailable(
                            service = "SynchronizationRepository",
                            retryAfter = null,
                        )
                    else ->
                        DeviceSynchronizationContractError.SystemError.ServiceUnavailable(
                            service = "DeviceSynchronizationService",
                            retryAfter = null,
                        )
                }
            }
            .map {
                RegisterDeviceResult(
                    deviceId = deviceId.value,
                    deviceName = command.deviceName,
                    registeredAt = Clock.System.now(),
                )
            }
    }

    override suspend fun executeSynchronization(command: SynchronizeCommand): Either<DeviceSynchronizationContractError, SynchronizationResult> {
        // Map contract strategy to domain strategy
        val domainStrategy = when (command.conflictResolutionStrategy) {
            ConflictResolutionStrategy.KEEP_LOCAL -> DomainConflictResolutionStrategy.LOCAL_WINS
            ConflictResolutionStrategy.KEEP_REMOTE -> DomainConflictResolutionStrategy.REMOTE_WINS
            ConflictResolutionStrategy.LATEST_TIMESTAMP -> DomainConflictResolutionStrategy.LAST_WRITE_WINS
            ConflictResolutionStrategy.MANUAL_REVIEW -> DomainConflictResolutionStrategy.MANUAL
        }

        return synchronizeHandler(
            SynchronizeDevice(
                remoteDeviceId = command.remoteDeviceId,
                since = null, // The contract doesn't have a since parameter
                conflictStrategy = domainStrategy,
            ),
        )
            .mapLeft { error ->
                when (error) {
                    is io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.ValidationError ->
                        when (error.validationRule) {
                            io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.ValidationRule.INVALID_STATE ->
                                DeviceSynchronizationContractError.BusinessError.DeviceNotFound(
                                    deviceId = command.remoteDeviceId,
                                    searchContext = "Remote device for synchronization",
                                )
                            else ->
                                DeviceSynchronizationContractError.InputError.InvalidDeviceId(
                                    deviceId = command.remoteDeviceId,
                                    expectedFormat = "Valid device ID format",
                                )
                        }
                    is io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.SyncOperationError ->
                        when (error.failureReason) {
                            io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.SyncFailureReason.NETWORK_ERROR,
                            io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.SyncFailureReason.TIMEOUT,
                            ->
                                DeviceSynchronizationContractError.BusinessError.SynchronizationFailed(
                                    failure = DeviceSynchronizationContractError.SynchronizationFailureType.NetworkFailure(
                                        localDeviceId = command.localDeviceId,
                                        remoteDeviceId = command.remoteDeviceId,
                                        lastSuccessfulSync = null,
                                    ),
                                )
                            io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.SyncFailureReason.AUTHENTICATION_FAILED ->
                                DeviceSynchronizationContractError.BusinessError.SynchronizationFailed(
                                    failure = DeviceSynchronizationContractError.SynchronizationFailureType.AuthenticationFailure(
                                        localDeviceId = command.localDeviceId,
                                        remoteDeviceId = command.remoteDeviceId,
                                        reason = DeviceSynchronizationContractError.AuthFailureReason.InvalidCredentials,
                                    ),
                                )
                            io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.SyncFailureReason.VERSION_MISMATCH ->
                                DeviceSynchronizationContractError.BusinessError.SynchronizationFailed(
                                    failure = DeviceSynchronizationContractError.SynchronizationFailureType.ProtocolMismatch(
                                        localVersion = "1.0",
                                        remoteVersion = "unknown",
                                        minimumRequiredVersion = "1.0",
                                    ),
                                )
                            io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.SyncFailureReason.DATA_CORRUPTION ->
                                DeviceSynchronizationContractError.BusinessError.SynchronizationFailed(
                                    failure = DeviceSynchronizationContractError.SynchronizationFailureType.DataCorruption(
                                        localDeviceId = command.localDeviceId,
                                        remoteDeviceId = command.remoteDeviceId,
                                        corruptedEventIds = emptyList(),
                                    ),
                                )
                            else ->
                                DeviceSynchronizationContractError.SystemError.ServiceUnavailable(
                                    service = "DeviceSynchronizationService",
                                    retryAfter = null,
                                )
                        }
                    is io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError.RepositoryError ->
                        DeviceSynchronizationContractError.SystemError.ServiceUnavailable(
                            service = "SynchronizationRepository",
                            retryAfter = null,
                        )
                    else ->
                        DeviceSynchronizationContractError.SystemError.ServiceUnavailable(
                            service = "DeviceSynchronizationService",
                            retryAfter = null,
                        )
                }
            }
            .map { result ->
                // Convert domain conflicts to contract conflicts
                val conflicts = if (result.conflictsDetected > 0) {
                    // Simplified - would need actual conflict details
                    listOf(
                        ConflictResult(
                            eventId = "example-conflict",
                            resolution = ConflictResolution.MANUAL_REVIEW_REQUIRED,
                            reason = "Conflict detected during synchronization",
                        ),
                    )
                } else {
                    emptyList()
                }

                SynchronizationResult(
                    eventsPushed = result.eventsPushed,
                    eventsPulled = result.eventsPulled,
                    conflicts = conflicts,
                    synchronizedAt = result.syncedAt,
                )
            }
    }
}
