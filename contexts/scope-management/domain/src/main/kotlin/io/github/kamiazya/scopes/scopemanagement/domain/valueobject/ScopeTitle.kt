package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError

/**
 * Value object representing a scope title with embedded validation.
 * Encapsulates the business rules for scope titles following DDD principles.
 */
@JvmInline
value class ScopeTitle private constructor(private val data: Pair<String, String>) {
    val value: String get() = data.first
    internal val normalizedValue: String get() = data.second

    private constructor(value: String, normalized: String) : this(value to normalized)

    companion object {
        const val MAX_LENGTH = 200
        const val MIN_LENGTH = 1

        /**
         * Create a validated ScopeTitle from a string.
         * Returns Either with specific error type or valid ScopeTitle.
         */
        fun create(title: String): Either<ScopeInputError.TitleError, ScopeTitle> = either {
            // Check for prohibited characters in the original input
            ensure(!title.contains('\n') && !title.contains('\r')) {
                ScopeInputError.TitleError.InvalidTitleFormat(title)
            }

            val trimmedTitle = title.trim()
            val normalizedTitle = normalize(trimmedTitle)

            ensure(trimmedTitle.isNotBlank()) {
                ScopeInputError.TitleError.EmptyTitle
            }
            // MIN_LENGTH is currently 1, making this check unreachable after isNotBlank().
            // However, it's included to support future increases to MIN_LENGTH and is used
            // in recovery logic and formatting utilities.
            ensure(trimmedTitle.length >= MIN_LENGTH) {
                ScopeInputError.TitleError.EmptyTitle
            }
            ensure(trimmedTitle.length <= MAX_LENGTH) {
                ScopeInputError.TitleError.TitleTooLong(MAX_LENGTH)
            }

            ScopeTitle(trimmedTitle, normalizedTitle)
        }

        /**
         * Normalizes a title for consistent comparison by:
         * - Trimming leading and trailing whitespace
         * - Collapsing internal whitespace sequences to single spaces
         * - Converting to lowercase using locale-invariant conversion
         *
         * This is internal to the value object, ensuring encapsulation.
         */
        private fun normalize(title: String): String = title.trim()
            .replace(Regex("\\s+"), " ")
            .lowercaseInvariant()

        /**
         * Converts a string to lowercase using locale-invariant rules.
         * Prevents locale-specific issues like the Turkish-I problem.
         */
        private fun String.lowercaseInvariant(): String = this.map { char ->
            when (char) {
                in 'A'..'Z' -> char + ('a' - 'A')
                else -> char
            }
        }.joinToString("")
    }

    override fun toString(): String = value

    /**
     * Checks equality based on normalized values for consistent comparison.
     * This ensures that "My Task" and "my  task" are considered equal.
     */
    fun equalsIgnoreCase(other: ScopeTitle): Boolean = this.normalizedValue == other.normalizedValue
}
