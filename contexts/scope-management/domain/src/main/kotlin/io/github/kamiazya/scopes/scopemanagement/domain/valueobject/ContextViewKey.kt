package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import kotlinx.datetime.Clock
/**
 * Value object representing a unique key for a context view.
 * The key is used for programmatic access and must follow specific naming conventions.
 *
 * Business rules:
 * - Must not be empty or blank
 * - Must be between 2 and 50 characters
 * - Must contain only lowercase letters, numbers, hyphens, and underscores
 * - Must start with a letter
 * - Cannot end with a hyphen or underscore
 */
@JvmInline
value class ContextViewKey private constructor(val value: String) {
    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 50
        private val VALID_PATTERN = Regex("^[a-z][a-z0-9_-]*[a-z0-9]$|^[a-z]$")

        /**
         * Create a ContextViewKey from a string with validation.
         * Returns Either with validation error or valid ContextViewKey.
         */
        fun create(value: String): Either<ContextError, ContextViewKey> = either {
            val trimmed = value.trim()

            ensure(trimmed.isNotEmpty()) { ContextError.EmptyKey(occurredAt = Clock.System.now()) }
            ensure(trimmed.length >= MIN_LENGTH) { ContextError.KeyTooShort(MIN_LENGTH, occurredAt = Clock.System.now()) }
            ensure(trimmed.length <= MAX_LENGTH) { ContextError.KeyTooLong(MAX_LENGTH, occurredAt = Clock.System.now()) }
            ensure(VALID_PATTERN.matches(trimmed)) {
                ContextError.InvalidKeyFormat(
                    errorType = ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN,
                    occurredAt = Clock.System.now(),
                )
            }

            ContextViewKey(trimmed)
        }
    }

    override fun toString(): String = value
}
