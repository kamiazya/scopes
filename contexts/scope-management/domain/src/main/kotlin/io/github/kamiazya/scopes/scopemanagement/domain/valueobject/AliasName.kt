package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError

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
        private val VALID_PATTERN = Regex("^[a-z]([a-z0-9-_]*[a-z0-9])?$")
        private val CONSECUTIVE_SPECIAL_CHARS = Regex("[-_]{2,}")

        fun create(value: String): Either<ScopeInputError.AliasError, AliasName> {
            val trimmed = value.trim().lowercase() // Normalize to lowercase

            if (trimmed.isBlank()) {
                return ScopeInputError.AliasError.EmptyAlias.left()
            }

            if (trimmed.length < MIN_LENGTH) {
                return ScopeInputError.AliasError.AliasTooShort(
                    minLength = MIN_LENGTH,
                ).left()
            }

            if (trimmed.length > MAX_LENGTH) {
                return ScopeInputError.AliasError.AliasTooLong(
                    maxLength = MAX_LENGTH,
                ).left()
            }

            if (!VALID_PATTERN.matches(trimmed)) {
                return ScopeInputError.AliasError.InvalidAliasFormat(
                    alias = trimmed,
                    expectedPattern = ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
                ).left()
            }

            if (CONSECUTIVE_SPECIAL_CHARS.containsMatchIn(trimmed)) {
                return ScopeInputError.AliasError.InvalidAliasFormat(
                    alias = trimmed,
                    expectedPattern = ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
                ).left()
            }

            return AliasName(trimmed).right()
        }
    }

    override fun toString(): String = value
}
