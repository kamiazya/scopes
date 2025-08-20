package io.github.kamiazya.scopes.application.error

sealed class AspectError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data object KeyEmpty : AspectError()
    data class KeyInvalidFormat(val attemptedKey: String, val expectedPattern: String) : AspectError()
    data class KeyReserved(val attemptedKey: String) : AspectError()

    data class ValueEmpty(val aspectKey: String) : AspectError()
    data class ValueNotInAllowedValues(
        val aspectKey: String,
        val attemptedValue: String,
        val allowedValues: List<String>,
    ) : AspectError()
}
