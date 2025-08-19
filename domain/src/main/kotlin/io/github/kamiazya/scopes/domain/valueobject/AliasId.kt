package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import kotlinx.datetime.Clock

/**
 * Value object representing a unique identifier for scope aliases.
 * Uses ULID (Universally Unique Lexicographically Sortable Identifier) format.
 *
 * This ID is immutable and allows tracking aliases even when their names change.
 */
@JvmInline
value class AliasId private constructor(val value: String) {

    companion object {
        /**
         * Creates a new AliasId from a string value.
         * Validates that the string is a valid ULID format.
         */
        fun create(value: String): Either<ScopeInputError.IdError, AliasId> {
            val trimmed = value.trim()

            if (trimmed.isBlank()) {
                return ScopeInputError.IdError.Blank(
                    occurredAt = Clock.System.now(),
                    attemptedValue = value
                ).left()
            }

            return if (ULID.isValid(trimmed)) {
                AliasId(trimmed).right()
            } else {
                ScopeInputError.IdError.InvalidFormat(
                    occurredAt = Clock.System.now(),
                    attemptedValue = trimmed,
                    expectedFormat = "ULID"
                ).left()
            }
        }

        /**
         * Generates a new unique AliasId using ULID.
         * ULIDs are time-ordered and globally unique.
         */
        fun generate(): AliasId {
            return AliasId(ULID.random())
        }
    }

    /**
     * Convert this AliasId to its corresponding AggregateId.
     *
     * @return Either an error or the AggregateId in URI format
     */
    fun toAggregateId(): Either<AggregateIdError, AggregateId> =
        AggregateId.create("ScopeAlias", value)

    override fun toString(): String = value
}
