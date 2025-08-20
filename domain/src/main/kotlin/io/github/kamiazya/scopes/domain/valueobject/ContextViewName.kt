package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right

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
        fun create(value: String): Either<String, ContextViewName> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() -> "Context view name cannot be empty".left()
                trimmed.length > MAX_LENGTH -> "Context view name cannot exceed $MAX_LENGTH characters".left()
                else -> ContextViewName(trimmed).right()
            }
        }
    }
}
