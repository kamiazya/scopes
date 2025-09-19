package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectKeyError

/**
 * Value object representing an aspect key.
 * Aspects are key-value pairs that provide metadata about a scope.
 */
@JvmInline
value class AspectKey private constructor(val value: String) {
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 50
        private val VALID_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")

        /**
         * Creates an AspectKey with validation.
         */
        fun create(value: String): Either<AspectKeyError, AspectKey> = when {
            value.isBlank() -> AspectKeyError.EmptyKey.left()
            value.length < MIN_LENGTH -> AspectKeyError.TooShort(actualLength = value.length, minLength = MIN_LENGTH).left()
            value.length > MAX_LENGTH -> AspectKeyError.TooLong(actualLength = value.length, maxLength = MAX_LENGTH).left()
            !VALID_PATTERN.matches(value) -> AspectKeyError.InvalidFormat.left()
            else -> AspectKey(value).right()
        }
    }

    override fun toString(): String = value
}
