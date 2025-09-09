package io.github.kamiazya.scopes.agentmanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.agentmanagement.domain.error.AgentIdError

/**
 * Strongly-typed identifier for Agent entities using ULID.
 *
 * ULIDs provide lexicographically sortable, globally unique identifiers
 * suitable for distributed systems.
 */
@JvmInline
value class AgentId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new ULID-based AgentId.
         */
        fun generate(): AgentId = AgentId(ULID.random())

        /**
         * Create AgentId from existing ULID string.
         *
         * @param value Valid ULID string
         * @return Either<AgentIdError, AgentId>
         */
        fun from(value: String): Either<AgentIdError, AgentId> = if (isValidUlid(value)) {
            AgentId(value).right()
        } else {
            AgentIdError.InvalidFormat(
                providedValue = value,
                expectedFormat = "26-character ULID string",
            ).left()
        }

        private fun isValidUlid(value: String): Boolean = value.length == 26 && value.all { it.isLetterOrDigit() }
    }

    override fun toString(): String = value
}
