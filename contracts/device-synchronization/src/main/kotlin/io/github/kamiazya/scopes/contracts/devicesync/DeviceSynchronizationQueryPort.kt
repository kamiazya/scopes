package io.github.kamiazya.scopes.contracts.devicesync

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.devicesync.errors.DeviceSynchronizationContractError

/**
 * Public contract for device synchronization read operations (Queries).
 * Following CQRS principles, this port handles only operations that read data without side effects.
 * 
 * Note: Currently this context is command-focused, but this interface is provided
 * for architectural consistency and future query operations.
 */
public interface DeviceSynchronizationQueryPort {
    // Placeholder for future query operations
    // Examples might include:
    // - Get device sync status
    // - List registered devices
    // - Get sync history
}