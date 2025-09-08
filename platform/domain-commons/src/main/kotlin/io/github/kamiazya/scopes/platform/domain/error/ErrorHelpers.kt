package io.github.kamiazya.scopes.platform.domain.error

import io.github.kamiazya.scopes.platform.commons.time.Instant
import io.github.kamiazya.scopes.platform.commons.time.TimeProvider
import kotlinx.datetime.Clock

/**
 * Helper function to create errors with timestamps.
 * Requires a TimeProvider to be injected for clean architecture compliance.
 */
fun createErrorWithTimestamp(timeProvider: TimeProvider): Instant = timeProvider.now()

/**
 * Legacy helper function for backward compatibility.
 * @deprecated Use TimeProvider injection pattern for clean architecture
 */
@Deprecated("Use TimeProvider injection", ReplaceWith("SystemTimeProvider().now()"))
fun currentTimestamp(): Instant = Clock.System.now()
