package io.github.kamiazya.scopes.contracts.devicesync.commands

/**
 * Command to register a new device for synchronization.
 */
public data class RegisterDeviceCommand(val deviceName: String, val deviceType: String? = null)
