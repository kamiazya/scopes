@file:DomainLayer

package io.github.kamiazya.scopes.devicesync.domain

import org.jmolecules.architecture.layered.DomainLayer

/**
 * Device Synchronization domain package.
 *
 * This bounded context handles multi-device consistency and synchronization.
 * It models vector clocks, sync states, conflicts, and the synchronization protocol.
 *
 * Domain Types:
 * - DeviceId: Identifier for devices (jMolecules Identifier)
 * - SyncState: Entity tracking synchronization state per device
 * - SyncConflict: Entity representing detected conflicts
 * - VectorClock: Value object for causal ordering
 *
 * Domain Services:
 * - DeviceSynchronizationService: Core sync logic
 */
