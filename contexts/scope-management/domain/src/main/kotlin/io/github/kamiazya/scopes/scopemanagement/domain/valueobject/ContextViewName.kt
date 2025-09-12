package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError

/**
 * Value object representing a context view name.
 *
 * Constraints:
 * - Cannot be empty
 * - Maximum length of 100 characters
 * - Must be trimmed (no leading/trailing whitespace)
 */
@JvmInline
value class ContextViewName private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH = 100

        /**
         * Create a ContextViewName from a string value.
         *
         * @param value The context name
         * @return Either an error or the ContextViewName
         */
        fun create(value: String): Either<ContextError, ContextViewName> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() -> ContextError.EmptyName(
                    attemptedValue = value,
                ).left()
                trimmed.length > MAX_LENGTH -> ContextError.NameTooLong(
                    attemptedValue = value,
                    maximumLength = MAX_LENGTH,
                ).left()
                else -> ContextViewName(trimmed).right()
            }
        }
    }
}
