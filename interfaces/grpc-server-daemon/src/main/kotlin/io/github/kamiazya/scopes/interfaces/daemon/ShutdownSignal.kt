package io.github.kamiazya.scopes.interfaces.daemon

/**
 * Signal containing shutdown request details.
 *
 * @property reason The reason for shutdown
 * @property gracePeriodSeconds Grace period in seconds for graceful shutdown
 * @property saveState Whether to save state before shutting down
 */
data class ShutdownSignal(val reason: String = "", val gracePeriodSeconds: Int = 5, val saveState: Boolean = true)
