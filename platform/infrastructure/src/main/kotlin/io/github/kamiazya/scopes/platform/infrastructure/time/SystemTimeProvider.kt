package io.github.kamiazya.scopes.platform.infrastructure.time

import io.github.kamiazya.scopes.platform.commons.time.Instant
import io.github.kamiazya.scopes.platform.commons.time.TimeProvider
import kotlinx.datetime.Clock

/**
 * System time provider that uses the system clock.
 * This is the production implementation of TimeProvider.
 */
class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Clock.System.now()
}
