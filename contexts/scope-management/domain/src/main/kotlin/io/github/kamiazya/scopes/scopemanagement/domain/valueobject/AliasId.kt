package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import org.jmolecules.ddd.types.Identifier

/**
 * Value object representing a unique identifier for scope aliases.
 * Uses ULID (Universally Unique Lexicographically Sortable Identifier) format.
 *
 * This ID is immutable and allows tracking aliases even when their names change.
 *
 */
@JvmInline
value class AliasId private constructor(val value: String) : Identifier {

    companion object {
        /**
         * Creates a new AliasId from a string value.
         * Validates that the string is a valid ULID format.
         */
        fun create(value: String): Either<ScopeInputError.IdError, AliasId> {
            val trimmed = value.trim()

            if (trimmed.isBlank()) {
                return ScopeInputError.IdError.EmptyId.left()
            }

            return if (ULID.isValid(trimmed)) {
                // Store in uppercase for consistency
                AliasId(trimmed.uppercase()).right()
            } else {
                ScopeInputError.IdError.InvalidIdFormat(
                    id = trimmed,
                    expectedFormat = ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID,
                ).left()
            }
        }

        /**
         * Generates a new unique AliasId using ULID.
         * ULIDs are time-ordered and globally unique.
         */
        fun generate(): AliasId = AliasId(ULID.generate().toString())
    }

    override fun toString(): String = value

    fun toAggregateId(): Either<AggregateIdError, AggregateId> = AggregateId.Uri.create(
        aggregateType = "Alias", // Changed from "ScopeAlias" to match test expectations
        id = value,
    ).mapLeft {
        AggregateIdError.InvalidFormat(
            value = value,
            formatError = AggregateIdError.FormatError.MALFORMED_URI,
        )
    }
}
