package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import kotlinx.datetime.Clock

/**
 * Value object representing a scope alias name.
 *
 * Business Rules:
 * - Must not be blank
 * - Must be between 2 and 64 characters
 * - Must contain only alphanumeric characters, hyphens, and underscores
 * - Must not start or end with hyphen or underscore
 * - Must not contain consecutive hyphens or underscores
 */
@JvmInline
value class AliasName private constructor(val value: String) {

    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 64
        private val VALID_PATTERN = Regex("^[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")
        private val CONSECUTIVE_SPECIAL_CHARS = Regex("[-_]{2,}")

        fun create(value: String): Either<ScopeInputError.AliasError, AliasName> {
            val trimmed = value.trim()

            if (trimmed.isBlank()) {
                return ScopeInputError.AliasError.Empty(
                    occurredAt = Clock.System.now(),
                    attemptedValue = value,
                ).left()
            }

            if (trimmed.length < MIN_LENGTH) {
                return ScopeInputError.AliasError.TooShort(
                    occurredAt = Clock.System.now(),
                    attemptedValue = trimmed,
                    minimumLength = MIN_LENGTH,
                ).left()
            }

            if (trimmed.length > MAX_LENGTH) {
                return ScopeInputError.AliasError.TooLong(
                    occurredAt = Clock.System.now(),
                    attemptedValue = trimmed,
                    maximumLength = MAX_LENGTH,
                ).left()
            }

            if (!VALID_PATTERN.matches(trimmed)) {
                return ScopeInputError.AliasError.InvalidFormat(
                    occurredAt = Clock.System.now(),
                    attemptedValue = trimmed,
                    expectedPattern = "alphanumeric, hyphens, underscores only; no consecutive special chars",
                ).left()
            }

            if (CONSECUTIVE_SPECIAL_CHARS.containsMatchIn(trimmed)) {
                return ScopeInputError.AliasError.InvalidFormat(
                    occurredAt = Clock.System.now(),
                    attemptedValue = trimmed,
                    expectedPattern = "no consecutive hyphens or underscores",
                ).left()
            }

            return AliasName(trimmed).right()
        }
    }

    override fun toString(): String = value
}
