package io.github.kamiazya.scopes.platform.domain.time

import io.github.kamiazya.scopes.platform.commons.time.TimeProvider
import kotlinx.datetime.Instant

/**
 * Test time provider that allows controlling time for deterministic testing.
 * This implementation is useful for domain testing where time needs to be controlled.
 */
class TestTimeProvider(private var currentTime: Instant = Instant.fromEpochMilliseconds(0)) : TimeProvider {

    override fun now(): Instant = currentTime

    fun setTime(time: Instant) {
        currentTime = time
    }

    fun advanceBy(milliseconds: Long) {
        currentTime = Instant.fromEpochMilliseconds(currentTime.toEpochMilliseconds() + milliseconds)
    }
}
