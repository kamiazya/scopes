package io.github.kamiazya.scopes.platform.commons.time

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

typealias Instant = Instant

object TimeProvider {
    fun now(): Instant = Clock.System.now()
}
