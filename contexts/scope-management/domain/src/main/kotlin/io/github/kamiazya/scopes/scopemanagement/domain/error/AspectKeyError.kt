package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Errors that can occur when creating an AspectKey.
 */
sealed class AspectKeyError : ScopesError() {
    override val occurredAt: Instant = Clock.System.now()

    data object EmptyKey : AspectKeyError()
    data class TooShort(val actualLength: Int, val minLength: Int) : AspectKeyError()
    data class TooLong(val actualLength: Int, val maxLength: Int) : AspectKeyError()
    data object InvalidFormat : AspectKeyError()
}
