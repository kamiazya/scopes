package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import kotlinx.datetime.Instant

/**
 * Provider for current time, allowing for testability and time manipulation in tests.
 */
interface TimeProvider {
    /**
     * Get the current instant.
     */
    fun now(): Instant
}

/**
 * Default implementation using system clock.
 */
class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = kotlinx.datetime.Clock.System.now()
}

/**
 * Test implementation allowing for fixed time.
 */
class FixedTimeProvider(private val fixedTime: Instant) : TimeProvider {
    override fun now(): Instant = fixedTime
}
