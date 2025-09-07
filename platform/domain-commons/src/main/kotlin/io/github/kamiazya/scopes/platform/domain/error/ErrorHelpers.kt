package io.github.kamiazya.scopes.platform.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Timestamp provider interface for error creation.
 * Enables dependency injection and testing with controlled time.
 */
interface TimestampProvider {
    fun now(): Instant
}

/**
 * System implementation of TimestampProvider using Clock.System.now().
 * Should be injected at the infrastructure layer boundary.
 */
class SystemTimestampProvider : TimestampProvider {
    override fun now(): Instant = Clock.System.now()
}

/**
 * Fixed timestamp provider for testing.
 */
class FixedTimestampProvider(private val fixedTime: Instant) : TimestampProvider {
    override fun now(): Instant = fixedTime
}

/**
 * Helper function to create errors with timestamps.
 * Requires a TimestampProvider to be injected for clean architecture compliance.
 */
fun createErrorWithTimestamp(timestampProvider: TimestampProvider): Instant = timestampProvider.now()

/**
 * Legacy helper function for backward compatibility.
 * @deprecated Use TimestampProvider injection pattern for clean architecture
 */
@Deprecated("Use TimestampProvider injection", ReplaceWith("SystemTimestampProvider().now()"))
fun currentTimestamp(): Instant = Clock.System.now()
