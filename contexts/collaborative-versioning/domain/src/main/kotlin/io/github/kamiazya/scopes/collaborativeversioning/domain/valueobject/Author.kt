package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.AuthorError

/**
 * Represents the author of a change proposal or review comment.
 * Can be either a human user or an AI agent.
 */
sealed class Author {
    abstract val id: String
    abstract val type: AuthorType
    abstract val displayName: String

    /**
     * Human user author.
     * TODO: Replace userId String with UserId value object when user management context is implemented.
     */
    data class User(val userId: String, override val displayName: String) : Author() {
        override val id: String = userId
        override val type: AuthorType = AuthorType.USER
    }

    /**
     * AI agent author.
     */
    data class Agent(val agentId: AgentId, override val displayName: String) : Author() {
        override val id: String = agentId.value
        override val type: AuthorType = AuthorType.AGENT
    }

    companion object {
        /**
         * Create an Author from a user ID.
         * TODO: Accept UserId value object when user management context is implemented.
         */
        fun fromUser(userId: String, displayName: String): Either<AuthorError, Author> = either {
            ensure(userId.isNotBlank()) {
                AuthorError.EmptyId
            }
            ensure(displayName.isNotBlank()) {
                AuthorError.EmptyDisplayName
            }
            User(userId, displayName)
        }

        /**
         * Create an Author from an agent ID.
         */
        fun fromAgent(agentId: AgentId, displayName: String): Either<AuthorError, Author> = either {
            ensure(displayName.isNotBlank()) {
                AuthorError.EmptyDisplayName
            }
            Agent(agentId, displayName)
        }

        /**
         * Create an Author from a string ID and type.
         * This is useful for deserialization scenarios.
         */
        fun from(id: String, type: AuthorType, displayName: String): Either<AuthorError, Author> = either {
            ensure(id.isNotBlank()) {
                AuthorError.EmptyId
            }
            ensure(displayName.isNotBlank()) {
                AuthorError.EmptyDisplayName
            }

            when (type) {
                AuthorType.USER -> {
                    // TODO: Validate with UserId.from() when available
                    User(id, displayName)
                }
                AuthorType.AGENT -> {
                    val agentId = AgentId.from(id)
                        .mapLeft { AuthorError.InvalidAgentId(id) }
                        .bind()
                    Agent(agentId, displayName)
                }
            }
        }
    }
}

/**
 * Type of author.
 */
enum class AuthorType {
    USER,
    AGENT,
}
