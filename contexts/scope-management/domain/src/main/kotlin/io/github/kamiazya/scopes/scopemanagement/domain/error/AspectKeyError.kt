package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Errors that can occur when creating an AspectKey.
 */
sealed class AspectKeyError : ScopesError() {
    override val occurredAt: Instant = Clock.System.now()

    data object EmptyKey : AspectKeyError()
    data class TooShort(val value: String, val minLength: Int) : AspectKeyError()
    data class TooLong(val value: String, val maxLength: Int) : AspectKeyError()
    data class InvalidFormat(val value: String) : AspectKeyError()
}
