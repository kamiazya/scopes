package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.AspectValidationError

/**
 * Value object representing an aspect key with embedded validation.
 * Aspect keys are used to identify different classification dimensions.
 * Examples: "priority", "status", "type", "complexity", "effort"
 */
@JvmInline
value class AspectKey private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 50
        const val MIN_LENGTH = 1

        /**
         * Create a validated AspectKey from a string.
         * Returns Either with validation error or valid AspectKey.
         */
        fun create(key: String): Either<AspectValidationError, AspectKey> = either {
            val trimmedKey = key.trim()

            ensure(trimmedKey.isNotBlank()) { AspectValidationError.EmptyAspectKey }
            ensure(trimmedKey.length >= MIN_LENGTH) { AspectValidationError.AspectKeyTooShort }
            ensure(trimmedKey.length <= MAX_LENGTH) {
                AspectValidationError.AspectKeyTooLong(MAX_LENGTH, trimmedKey.length)
            }
            ensure(trimmedKey.matches(Regex("^[a-z][a-z0-9_]*$"))) {
                AspectValidationError.InvalidAspectKeyFormat
            }

            AspectKey(trimmedKey)
        }
    }

    override fun toString(): String = value
}

