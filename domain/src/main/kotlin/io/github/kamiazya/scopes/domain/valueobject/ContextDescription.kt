package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Value object representing a context view description with embedded validation.
 * Context descriptions provide additional information about the purpose and usage of a context view.
 */
@JvmInline
value class ContextDescription private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 500
        const val MIN_LENGTH = 1

        /**
         * Create a validated ContextDescription from a string.
         * Returns Either with validation error or valid ContextDescription.
         */
        fun create(description: String): Either<String, ContextDescription> {
            val trimmedDescription = description.trim()

            if (trimmedDescription.isEmpty()) {
                return "Context description cannot be empty".left()
            }

            if (trimmedDescription.length < MIN_LENGTH) {
                return "Context description must be at least $MIN_LENGTH character".left()
            }

            if (trimmedDescription.length > MAX_LENGTH) {
                return "Context description cannot exceed $MAX_LENGTH characters".left()
            }

            return ContextDescription(trimmedDescription).right()
        }

        /**
         * Create an optional ContextDescription from a nullable string.
         * Returns null if input is null or blank, otherwise validates and returns ContextDescription.
         */
        fun createOptional(description: String?): Either<String, ContextDescription?> {
            if (description.isNullOrBlank()) {
                return null.right()
            }

            return create(description).map { it }
        }
    }

    override fun toString(): String = value
}
