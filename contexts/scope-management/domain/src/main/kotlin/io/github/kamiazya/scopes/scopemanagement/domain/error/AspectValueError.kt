package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Errors that can occur when creating an AspectValue.
 */
sealed class AspectValueError : ScopesError() {
    override val occurredAt: Instant = Clock.System.now()

    data object EmptyValue : AspectValueError()
    data class TooLong(val value: String, val maxLength: Int) : AspectValueError()
}
