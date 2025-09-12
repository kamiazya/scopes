package io.github.kamiazya.scopes.agentmanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.agentmanagement.domain.error.AgentNameError

/**
 * Value object representing an agent's name.
 * Ensures name is valid according to business rules.
 */
@JvmInline
value class AgentName private constructor(val value: String) {
    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 100
        private val VALID_PATTERN = Regex("^[a-zA-Z0-9_\\-. ]+$")

        /**
         * Create AgentName from string value with validation.
         *
         * @param value The agent name string
         * @return Either<AgentNameError, AgentName>
         */
        fun create(value: String): Either<AgentNameError, AgentName> = either {
            val trimmedName = value.trim()

            ensure(trimmedName.isNotEmpty()) {
                AgentNameError.EmptyName
            }

            ensure(trimmedName.length >= MIN_LENGTH) {
                AgentNameError.NameTooShort(
                    minLength = MIN_LENGTH,
                    actualLength = trimmedName.length,
                    name = trimmedName,
                )
            }

            ensure(trimmedName.length <= MAX_LENGTH) {
                AgentNameError.NameTooLong(
                    maxLength = MAX_LENGTH,
                    actualLength = trimmedName.length,
                    name = trimmedName,
                )
            }

            val invalidChars = trimmedName.filterNot { VALID_PATTERN.matches(it.toString()) }.toSet()
            ensure(invalidChars.isEmpty()) {
                AgentNameError.InvalidCharacters(
                    name = trimmedName,
                    invalidCharacters = invalidChars,
                )
            }

            AgentName(trimmedName)
        }
    }

    override fun toString(): String = value
}
