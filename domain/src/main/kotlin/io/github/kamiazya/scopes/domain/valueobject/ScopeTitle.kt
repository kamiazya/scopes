package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ScopeValidationError
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
        fun create(title: String): Either<ScopeValidationError, ScopeTitle> = either {
            val trimmedTitle = title.trim()

            ensure(trimmedTitle.isNotBlank()) { ScopeValidationError.EmptyScopeTitle }
            // MIN_LENGTH is currently 1, making this check unreachable after isNotBlank().
            // However, it's included to support future increases to MIN_LENGTH and is used
            // in recovery logic and formatting utilities.
            ensure(trimmedTitle.length >= MIN_LENGTH) { ScopeValidationError.ScopeTitleTooShort }
            ensure(trimmedTitle.length <= MAX_LENGTH) {
                ScopeValidationError.ScopeTitleTooLong(MAX_LENGTH, trimmedTitle.length)
            }
            ensure(!trimmedTitle.contains('\n') && !trimmedTitle.contains('\r')) {
                ScopeValidationError.ScopeTitleContainsNewline
            }

            ScopeTitle(trimmedTitle)
        }


    }

    override fun toString(): String = value
}
