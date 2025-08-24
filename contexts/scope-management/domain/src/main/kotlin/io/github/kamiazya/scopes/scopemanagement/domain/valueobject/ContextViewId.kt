package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import kotlinx.datetime.Clock

/**
 * Value object representing a unique identifier for a context view.
 * Uses ULID for lexicographically sortable distributed system compatibility.
 *
 * Follows functional error handling pattern with Either instead of exceptions.
 */
@JvmInline
value class ContextViewId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new unique ContextViewId with ULID format.
         */
        fun generate(): ContextViewId = ContextViewId(ULID.generate().toString())

        /**
         * Create a ContextViewId from an existing string value with validation.
         * Returns Either with specific error types instead of throwing exceptions.
         *
         * @param value The string value to validate and wrap
         * @return Either an error or a valid ContextViewId
         */
        fun create(value: String): Either<ContextError, ContextViewId> = either {
            ensure(value.isNotBlank()) {
                ContextError.BlankId(
                    occurredAt = Clock.System.now(),
                    attemptedValue = value,
                )
            }
            ensure(ULID.isValid(value)) {
                ContextError.InvalidIdFormat(
                    occurredAt = Clock.System.now(),
                    attemptedValue = value,
                    expectedFormat = "ULID",
                )
            }
            ContextViewId(value)
        }
    }

    override fun toString(): String = value

    /**
     * Converts this ContextViewId to an AggregateId.
     * Used when treating the context view as an aggregate root.
     */
    fun toAggregateId(): Either<
        io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError,
        io.github.kamiazya.scopes.platform.domain.value.AggregateId,
        > =
        io.github.kamiazya.scopes.platform.domain.value.AggregateId.Uri.create(
            aggregateType = "ContextView",
            id = value,
        ).mapLeft {
            io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError.InvalidFormat(
                occurredAt = Clock.System.now(),
                value = value,
                message = "Failed to create AggregateId from ContextViewId",
            )
        }
}
