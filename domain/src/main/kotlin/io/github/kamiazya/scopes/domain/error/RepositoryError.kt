package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Repository-specific errors for infrastructure concerns.
 * These errors represent technical failures rather than business rule violations.
 */
sealed class RepositoryError {
    data class ConnectionError(val cause: Throwable) : RepositoryError()
    data class DataIntegrityError(val message: String, val cause: Throwable? = null) : RepositoryError()
    data class NotFound(val id: ScopeId) : RepositoryError()
    data class ConflictError(val id: ScopeId, val message: String) : RepositoryError()
    data class SerializationError(val message: String, val cause: Throwable) : RepositoryError()
    data class DatabaseError(val message: String, val cause: Throwable? = null) : RepositoryError()
    data class UnknownError(val message: String, val cause: Throwable) : RepositoryError()
}
