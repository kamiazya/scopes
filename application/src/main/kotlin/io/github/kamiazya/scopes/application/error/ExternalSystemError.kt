package io.github.kamiazya.scopes.application.error

sealed class ExternalSystemError(recoverable: Boolean = false) : ApplicationError(recoverable) {
    data class ServiceUnavailable(
        val serviceName: String,
        val operation: String
    ) : ExternalSystemError()

    data class AuthenticationFailed(val serviceName: String) : ExternalSystemError()
}
