package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.DomainError
import kotlinx.serialization.Serializable

/**
 * Value object representing a scope title with embedded validation.
 * Encapsulates the business rules for scope titles following DDD principles.
 */
@Serializable
@JvmInline
value class ScopeTitle private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 200
        const val MIN_LENGTH = 1

        /**
         * Create a validated ScopeTitle from a string.
         * Returns Either with validation error or valid ScopeTitle.
         */
        fun create(title: String): Either<DomainError.ScopeValidationError, ScopeTitle> = either {
            val trimmedTitle = title.trim()

            ensure(trimmedTitle.isNotBlank()) { DomainError.ScopeValidationError.EmptyScopeTitle }
            ensure(trimmedTitle.length >= MIN_LENGTH) { DomainError.ScopeValidationError.ScopeTitleTooShort }
            ensure(trimmedTitle.length <= MAX_LENGTH) {
                DomainError.ScopeValidationError.ScopeTitleTooLong(MAX_LENGTH, trimmedTitle.length)
            }
            ensure(!trimmedTitle.contains('\n') && !trimmedTitle.contains('\r')) {
                DomainError.ScopeValidationError.ScopeTitleContainsNewline
            }

            ScopeTitle(trimmedTitle)
        }

        /**
         * Internal factory method for creating ScopeTitle without validation.
         * Used when title is already known to be valid (e.g., from database).
         */
        internal fun createUnchecked(title: String): ScopeTitle = ScopeTitle(title)
    }

    override fun toString(): String = value
}
