package io.github.kamiazya.scopes.contracts.devicesync.results

import kotlinx.datetime.Instant

/**
 * Result of device registration.
 */
public data class RegisterDeviceResult(val deviceId: String, val deviceName: String, val registeredAt: Instant)
