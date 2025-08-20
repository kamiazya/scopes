package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Value object representing a context view filter expression.
 *
 * Constraints:
 * - Cannot be empty
 * - Maximum length of 500 characters
 * - Must be trimmed (no leading/trailing whitespace)
 *
 * A filter expression defines criteria for including scopes in a context view.
 * Examples: "status:active", "priority:high AND tag:urgent", etc.
 */
@JvmInline
value class ContextViewFilter private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH = 500

        /**
         * Create a ContextViewFilter from a string value.
         *
         * @param value The filter expression
         * @return Either an error or the ContextViewFilter
         */
        fun create(value: String): Either<String, ContextViewFilter> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() -> "Context view filter cannot be empty".left()
                trimmed.length > MAX_LENGTH -> "Context view filter cannot exceed $MAX_LENGTH characters".left()
                else -> ContextViewFilter(trimmed).right()
            }
        }

        /**
         * Create an empty filter that matches all scopes.
         */
        fun all(): ContextViewFilter = ContextViewFilter("*")
    }
}
