package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.error.currentTimestamp

/**
 * Type-safe identifier for Scope entities using ULID for lexicographically sortable distributed system compatibility.
 */
@JvmInline
value class ScopeId private constructor(
    val value: String,
) {
    companion object {
        /**
         * Generate a new random ScopeId with ULID format.
         */
        fun generate(): ScopeId = ScopeId(ULID.random())

        /**
         * Create a ScopeId from a string value with validation.
         * Returns Either with specific error types instead of throwing exceptions.
         */
        fun create(value: String): Either<ScopeInputError.IdError, ScopeId> = either {
            ensure(value.isNotBlank()) {
                ScopeInputError.IdError.Blank(
                    currentTimestamp(),
                    value
                )
            }
            ensure(ULID.isValid(value)) {
                ScopeInputError.IdError.InvalidFormat(
                    currentTimestamp(),
                    value
                )
            }
            ScopeId(value)
        }

    }

    /**
     * Convert this ScopeId to its corresponding AggregateId.
     *
     * @return Either an error or the AggregateId in URI format
     */
    fun toAggregateId(): Either<AggregateIdError, AggregateId> =
        AggregateId.create("Scope", value)

    override fun toString(): String = value
}


