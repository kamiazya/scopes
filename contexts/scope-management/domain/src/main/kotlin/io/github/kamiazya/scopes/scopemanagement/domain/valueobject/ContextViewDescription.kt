package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
/**
 * Value object representing an optional context view description.
 * Provides additional information about the purpose and usage of a context view.
 */
@JvmInline
value class ContextViewDescription private constructor(val value: String) {

    companion object {
        private const val MAX_LENGTH = 500
        private const val MIN_LENGTH = 1

        /**
         * Create a validated ContextViewDescription from a string.
         * Returns Either with validation error or valid description.
         */
        fun create(value: String): Either<ContextError, ContextViewDescription> = either {
            val trimmedValue = value.trim()

            ensure(trimmedValue.isNotBlank()) { ContextError.EmptyDescription }
            ensure(trimmedValue.length >= MIN_LENGTH) { ContextError.DescriptionTooShort(MIN_LENGTH) }
            ensure(trimmedValue.length <= MAX_LENGTH) { ContextError.DescriptionTooLong(MAX_LENGTH) }

            ContextViewDescription(trimmedValue)
        }
    }

    override fun toString(): String = value
}
