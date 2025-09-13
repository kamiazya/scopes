package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ChangesetIdError

/**
 * Strongly-typed identifier for changesets using ULID.
 *
 * A changeset represents a set of changes made to a resource
 * by an agent at a specific point in time.
 */
@JvmInline
value class ChangesetId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new ULID-based ChangesetId.
         */
        fun generate(): ChangesetId = ChangesetId(ULID.random())

        /**
         * Create ChangesetId from existing ULID string.
         *
         * @param value Valid ULID string
         * @return Either<ChangesetIdError, ChangesetId>
         */
        fun from(value: String): Either<ChangesetIdError, ChangesetId> = if (isValidUlid(value)) {
            ChangesetId(value).right()
        } else {
            ChangesetIdError.InvalidFormat(
                providedValue = value,
                expectedFormat = "26-character ULID string",
            ).left()
        }

        private fun isValidUlid(value: String): Boolean = value.length == 26 && value.all { it.isLetterOrDigit() }
    }

    override fun toString(): String = value
}
