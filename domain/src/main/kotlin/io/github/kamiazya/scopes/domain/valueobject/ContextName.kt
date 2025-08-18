package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Value object representing the name of a context view.
 * Context names must be unique within their scope (global or local).
 *
 * Business rules:
 * - Must be between 1 and 50 characters
 * - Must start with a letter
 * - Can contain letters, numbers, dashes, and underscores
 * - Cannot contain spaces or special characters
 * - Case-insensitive for uniqueness (but preserves original case)
 */
@JvmInline
value class ContextName private constructor(val value: String) {

    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 50
        private val VALID_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")

        /**
         * Create a ContextName from a string value.
         * Returns Either.Left with error message if validation fails.
         */
        fun create(value: String): Either<String, ContextName> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() ->
                    "Context name cannot be empty".left()
                trimmed.length < MIN_LENGTH ->
                    "Context name must be at least $MIN_LENGTH character".left()
                trimmed.length > MAX_LENGTH ->
                    "Context name cannot exceed $MAX_LENGTH characters".left()
                !VALID_PATTERN.matches(trimmed) ->
                    "Context name must start with a letter and contain only letters, numbers, dashes, and underscores".left()
                else -> ContextName(trimmed).right()
            }
        }

        /**
         * Create a ContextName without validation (for internal use only).
         * Use with caution - only for trusted sources like database reads.
         */
        internal fun unsafeCreate(value: String): ContextName = ContextName(value)
    }

    /**
     * Get a normalized version for comparison (lowercase).
     */
    fun normalized(): String = value.lowercase()

    override fun toString(): String = value
}

