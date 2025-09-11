package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors that can occur when creating an AspectKey.
 */
sealed class AspectKeyError : ScopesError() {
    data class EmptyKey() : AspectKeyError()
    data class TooShort(val actualLength: Int, val minLength: Int) : AspectKeyError()
    data class TooLong(val actualLength: Int, val maxLength: Int) : AspectKeyError()
    data class InvalidFormat() : AspectKeyError()
}
