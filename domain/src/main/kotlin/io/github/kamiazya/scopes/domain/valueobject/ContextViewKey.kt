package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * ContextViewKey represents a unique identifier key for a ContextView.
 * Used for uniqueness constraints and programmatic access.
 * 
 * Constraints:
 * - Must start with a letter
 * - Can contain only letters, numbers, dashes, and underscores
 * - No spaces allowed
 * - Between 1 and 50 characters
 */
@JvmInline
value class ContextViewKey private constructor(val value: String) {
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 50
        private val VALID_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")

        /**
         * Create a ContextViewKey from a string value.
         * Returns Either.Left with error message if validation fails.
         */
        fun create(value: String): Either<String, ContextViewKey> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() ->
                    "Context view key cannot be empty".left()
                trimmed.length < MIN_LENGTH ->
                    "Context view key must be at least $MIN_LENGTH character".left()
                trimmed.length > MAX_LENGTH ->
                    "Context view key cannot exceed $MAX_LENGTH characters".left()
                !VALID_PATTERN.matches(trimmed) ->
                    "Context view key must start with a letter and contain only letters, numbers, dashes, and underscores".left()
                else -> ContextViewKey(trimmed).right()
            }
        }
    }
}