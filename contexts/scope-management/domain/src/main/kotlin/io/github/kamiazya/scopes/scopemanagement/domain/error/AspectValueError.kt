package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors that can occur when creating an AspectValue.
 */
sealed class AspectValueError : ScopesError() {
    data object EmptyValue : AspectValueError()
    data class TooLong(val actualLength: Int, val maxLength: Int) : AspectValueError()
}
