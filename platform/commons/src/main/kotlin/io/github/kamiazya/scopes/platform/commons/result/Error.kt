package io.github.kamiazya.scopes.platform.commons.result

sealed interface PlatformError {
    val message: String
    val cause: Throwable?
}

data class ValidationError(override val message: String, override val cause: Throwable? = null) : PlatformError

data class NotFoundError(override val message: String, override val cause: Throwable? = null) : PlatformError

data class ConflictError(override val message: String, override val cause: Throwable? = null) : PlatformError

data class UnexpectedError(override val message: String, override val cause: Throwable? = null) : PlatformError
