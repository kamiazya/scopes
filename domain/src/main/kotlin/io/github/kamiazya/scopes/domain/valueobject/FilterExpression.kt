package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.ContextError
import io.github.kamiazya.scopes.domain.error.currentTimestamp

/**
 * Value object representing a filter expression for context views.
 *
 * Filter expressions use a simple DSL for filtering scopes based on their aspects.
 * Examples:
 * - "status:active" - Scopes with status aspect set to "active"
 * - "priority:high AND status:in-progress" - Combination of filters
 * - "tag:bug OR tag:feature" - Either bug or feature tagged
 */
@JvmInline
value class FilterExpression private constructor(
    val value: String
) {
    companion object {
        /**
         * Create a FilterExpression with validation.
         *
         * @param value The filter expression string
         * @return Either an error or a valid FilterExpression
         */
        fun create(value: String): Either<ContextError.FilterError, FilterExpression> = either {
            ensure(value.isNotBlank()) {
                ContextError.FilterError.InvalidSyntax(
                    occurredAt = currentTimestamp(),
                    position = 0,
                    reason = "Filter expression cannot be blank",
                    expression = value
                )
            }

            // Basic validation - ensure it doesn't contain invalid characters
            ensure(!value.contains('\n') && !value.contains('\r')) {
                ContextError.FilterError.InvalidSyntax(
                    occurredAt = currentTimestamp(),
                    position = value.indexOfFirst { it == '\n' || it == '\r' },
                    reason = "Filter expression cannot contain newline characters",
                    expression = value
                )
            }

            // TODO: Add more sophisticated validation for filter syntax
            // For now, accept any non-blank string without newlines

            FilterExpression(value)
        }

        /**
         * Create a filter expression that matches all scopes.
         */
        fun all(): FilterExpression = FilterExpression("*")

        /**
         * Create a filter expression that matches no scopes.
         */
        fun none(): FilterExpression = FilterExpression("!*")
    }

    override fun toString(): String = value
}
