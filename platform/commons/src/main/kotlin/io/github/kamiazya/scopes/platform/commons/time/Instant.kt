package io.github.kamiazya.scopes.platform.commons.time

import kotlinx.datetime.Instant

typealias Instant = Instant

/**
 * Abstraction for time providers to support testability and dependency inversion.
 * Domain layers should depend on this interface rather than concrete time implementations.
 */
interface TimeProvider {
    fun now(): Instant
}
