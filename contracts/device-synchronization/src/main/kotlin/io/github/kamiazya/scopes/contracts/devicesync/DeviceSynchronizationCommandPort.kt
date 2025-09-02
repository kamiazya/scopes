package io.github.kamiazya.scopes.contracts.devicesync

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.devicesync.commands.RegisterDeviceCommand
import io.github.kamiazya.scopes.contracts.devicesync.commands.SynchronizeCommand
import io.github.kamiazya.scopes.contracts.devicesync.errors.DeviceSynchronizationContractError
import io.github.kamiazya.scopes.contracts.devicesync.results.RegisterDeviceResult
import io.github.kamiazya.scopes.contracts.devicesync.results.SynchronizationResult

/**
 * Public contract for device synchronization write operations (Commands).
 * Following CQRS principles, this port handles only operations that modify state.
 * All operations return Either for explicit error handling.
 */
public interface DeviceSynchronizationCommandPort {
    /**
     * Registers a new device for synchronization.
     * @param command The command containing device registration details
     * @return Either an error or the registered device result
     */
    public suspend fun registerDevice(command: RegisterDeviceCommand): Either<DeviceSynchronizationContractError, RegisterDeviceResult>

    /**
     * Synchronizes events between devices.
     * @param command The command containing synchronization parameters
     * @return Either an error or the synchronization result
     */
    public suspend fun executeSynchronization(command: SynchronizeCommand): Either<DeviceSynchronizationContractError, SynchronizationResult>
}