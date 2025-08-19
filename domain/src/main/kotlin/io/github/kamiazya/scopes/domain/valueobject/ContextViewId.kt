package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.error.currentTimestamp

/**
 * Value object representing a unique identifier for a context view.
 * Uses ULID for lexicographically sortable distributed system compatibility.
 * 
 * Follows functional error handling pattern with Either instead of exceptions.
 */
@JvmInline
value class ContextViewId private constructor(
    val value: String,
) {
    companion object {
        /**
         * Generate a new unique ContextViewId with ULID format.
         */
        fun generate(): ContextViewId = ContextViewId(ULID.random())

        /**
         * Create a ContextViewId from an existing string value with validation.
         * Returns Either with specific error types instead of throwing exceptions.
         * 
         * @param value The string value to validate and wrap
         * @return Either an error or a valid ContextViewId
         */
        fun create(value: String): Either<ScopeInputError.IdError, ContextViewId> = either {
            ensure(value.isNotBlank()) { 
                ScopeInputError.IdError.Blank(
                    currentTimestamp(),
                    value
                )
            }
            ensure(ULID.isValid(value)) { 
                ScopeInputError.IdError.InvalidFormat(
                    currentTimestamp(),
                    value,
                    "ULID"
                )
            }
            ContextViewId(value)
        }

        /**
         * Legacy factory method for backwards compatibility.
         * @deprecated Use create() for safer error handling
         */
        @Deprecated(
            message = "Use create() for safer error handling",
            replaceWith = ReplaceWith("create(value).getOrNull() ?: throw IllegalArgumentException()")
        )
        fun from(value: String): ContextViewId = 
            create(value).fold(
                ifLeft = { throw IllegalArgumentException("Invalid ContextViewId: $value") },
                ifRight = { it }
            )
    }

    /**
     * Convert this ContextViewId to its corresponding AggregateId.
     * 
     * @return Either an error or the AggregateId in URI format
     */
    fun toAggregateId(): Either<AggregateIdError, AggregateId> = 
        AggregateId.create("ContextView", value)

    override fun toString(): String = value
}
