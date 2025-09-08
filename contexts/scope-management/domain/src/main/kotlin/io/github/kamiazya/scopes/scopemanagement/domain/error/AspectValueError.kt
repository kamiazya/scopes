package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Errors that can occur when creating an AspectValue.
 */
sealed class AspectValueError : ScopesError() {
    data class EmptyValue(override val occurredAt: Instant) : AspectValueError()
    data class TooLong(val actualLength: Int, val maxLength: Int, override val occurredAt: Instant) : AspectValueError()
}
