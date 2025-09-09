package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ProposalIdError

/**
 * Strongly-typed identifier for change proposals using ULID.
 *
 * A change proposal represents a set of proposed changes to a resource
 * that must go through a review and approval process.
 */
@JvmInline
value class ProposalId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new ULID-based ProposalId.
         */
        fun generate(): ProposalId = ProposalId(ULID.random())

        /**
         * Create ProposalId from existing ULID string.
         *
         * @param value Valid ULID string
         * @return Either<ProposalIdError, ProposalId>
         */
        fun from(value: String): Either<ProposalIdError, ProposalId> = if (isValidUlid(value)) {
            ProposalId(value).right()
        } else {
            ProposalIdError.InvalidFormat(
                providedValue = value,
                expectedFormat = "26-character ULID string",
            ).left()
        }

        private fun isValidUlid(value: String): Boolean = value.length == 26 && value.all { it.isLetterOrDigit() }
    }

    override fun toString(): String = value
}
