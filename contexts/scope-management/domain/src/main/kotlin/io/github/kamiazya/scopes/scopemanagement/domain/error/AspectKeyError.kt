package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Errors that can occur when creating an AspectKey.
 */
sealed class AspectKeyError : ScopesError() {
    data class EmptyKey(override val occurredAt: Instant) : AspectKeyError()
    data class TooShort(val actualLength: Int, val minLength: Int, override val occurredAt: Instant) : AspectKeyError()
    data class TooLong(val actualLength: Int, val maxLength: Int, override val occurredAt: Instant) : AspectKeyError()
    data class InvalidFormat(override val occurredAt: Instant) : AspectKeyError()
}
