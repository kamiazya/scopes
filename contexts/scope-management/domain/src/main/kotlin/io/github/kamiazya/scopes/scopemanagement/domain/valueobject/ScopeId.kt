package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.currentTimestamp

/**
 * Type-safe identifier for Scope entities using ULID for lexicographically sortable distributed system compatibility.
 */
@JvmInline
value class ScopeId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new random ScopeId with ULID format.
         */
        fun generate(): ScopeId = ScopeId(ULID.generate().toString())

        /**
         * Create a ScopeId from a string value with validation.
         * Returns Either with specific error types instead of throwing exceptions.
         */
        fun create(value: String): Either<ScopeInputError.IdError, ScopeId> = either {
            ensure(value.isNotBlank()) {
                ScopeInputError.IdError.Blank(
                    currentTimestamp(),
                    value,
                )
            }
            ensure(ULID.isValid(value)) {
                ScopeInputError.IdError.InvalidFormat(
                    currentTimestamp(),
                    value,
                    ScopeInputError.IdError.InvalidFormat.IdFormatType.ULID,
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
    fun toAggregateId(): Either<AggregateIdError, AggregateId> = AggregateId.Uri.create(
        aggregateType = "Scope",
        id = value,
    ).mapLeft {
        AggregateIdError.InvalidFormat(
            occurredAt = currentTimestamp(),
            value = value,
            formatError = AggregateIdError.FormatError.MALFORMED_URI,
        )
    }

    override fun toString(): String = value
}
