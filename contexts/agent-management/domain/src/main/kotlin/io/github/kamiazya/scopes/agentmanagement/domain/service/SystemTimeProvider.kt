package io.github.kamiazya.scopes.agentmanagement.domain.service

import io.github.kamiazya.scopes.platform.commons.time.TimeProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Default implementation of TimeProvider using system clock.
 */
class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Clock.System.now()
}
