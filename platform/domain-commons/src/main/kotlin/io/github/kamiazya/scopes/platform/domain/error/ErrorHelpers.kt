package io.github.kamiazya.scopes.platform.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Helper function to get current timestamp for error creation.
 * This can be overridden for testing purposes.
 */
fun currentTimestamp(): Instant = Clock.System.now()
