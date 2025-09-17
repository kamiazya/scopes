package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.DomainValidationError

/**
 * Domain service for centralized validation logic.
 *
 * This service removes validation responsibility from the interface layer,
 * ensuring consistent validation across all interfaces (MCP, CLI, etc.).
 */
class ValidationService {

    companion object {
        private val ULID_REGEX = Regex("^[0-9A-HJKMNP-TV-Z]{26}$")
        private const val MIN_OFFSET = 0
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 1000
    }

    /**
     * Validate a ULID string.
     *
     * @param ulid The ULID string to validate
     * @return Either validation error or valid ULID
     */
    fun validateULID(ulid: String): Either<DomainValidationError.InvalidULID, String> = if (ulid.matches(ULID_REGEX)) {
        Either.Right(ulid)
    } else {
        Either.Left(DomainValidationError.InvalidULID(ulid))
    }

    /**
     * Check if a string is a valid ULID.
     *
     * @param value The string to check
     * @return true if valid ULID, false otherwise
     */
    fun isValidULID(value: String): Boolean = value.matches(ULID_REGEX)

    /**
     * Validate pagination parameters.
     *
     * @param offset The offset value
     * @param limit The limit value
     * @return Either validation error or valid pagination parameters
     */
    fun validatePagination(offset: Int, limit: Int): Either<DomainValidationError.InvalidPagination, Pair<Int, Int>> = when {
        offset < MIN_OFFSET ->
            Either.Left(DomainValidationError.InvalidPagination.OffsetTooSmall(offset, MIN_OFFSET))
        limit < MIN_LIMIT ->
            Either.Left(DomainValidationError.InvalidPagination.LimitTooSmall(limit, MIN_LIMIT))
        limit > MAX_LIMIT ->
            Either.Left(DomainValidationError.InvalidPagination.LimitTooLarge(limit, MAX_LIMIT))
        else ->
            Either.Right(offset to limit)
    }

    /**
     * Validate a non-empty string.
     *
     * @param value The string to validate
     * @param fieldName The name of the field for error messages
     * @return Either validation error or valid string
     */
    fun validateNonEmpty(value: String, fieldName: String): Either<DomainValidationError.EmptyField, String> = if (value.isNotBlank()) {
        Either.Right(value.trim())
    } else {
        Either.Left(DomainValidationError.EmptyField(fieldName))
    }

    /**
     * Validate a string length.
     *
     * @param value The string to validate
     * @param fieldName The name of the field
     * @param minLength Minimum allowed length (inclusive)
     * @param maxLength Maximum allowed length (inclusive)
     * @return Either validation error or valid string
     */
    fun validateLength(
        value: String,
        fieldName: String,
        minLength: Int = 0,
        maxLength: Int = Int.MAX_VALUE,
    ): Either<DomainValidationError.InvalidLength, String> = when {
        value.length < minLength ->
            Either.Left(DomainValidationError.InvalidLength(fieldName, value.length, minLength, maxLength))
        value.length > maxLength ->
            Either.Left(DomainValidationError.InvalidLength(fieldName, value.length, minLength, maxLength))
        else ->
            Either.Right(value)
    }

    /**
     * Validate an idempotency key.
     *
     * @param key The idempotency key to validate
     * @return Either validation error or valid key
     */
    fun validateIdempotencyKey(key: String): Either<DomainValidationError.InvalidIdempotencyKey, String> {
        // Idempotency keys should be UUIDs or similar unique identifiers
        val keyRegex = Regex("^[a-zA-Z0-9-_]{1,64}$")

        return when {
            key.isEmpty() ->
                Either.Left(
                    DomainValidationError.InvalidIdempotencyKey(
                        key,
                        DomainValidationError.InvalidIdempotencyKey.IdempotencyKeyError.TOO_SHORT,
                    ),
                )
            key.length > 64 ->
                Either.Left(
                    DomainValidationError.InvalidIdempotencyKey(
                        key,
                        DomainValidationError.InvalidIdempotencyKey.IdempotencyKeyError.TOO_LONG,
                    ),
                )
            !key.matches(keyRegex) ->
                Either.Left(
                    DomainValidationError.InvalidIdempotencyKey(
                        key,
                        DomainValidationError.InvalidIdempotencyKey.IdempotencyKeyError.INVALID_CHARACTERS,
                    ),
                )
            else -> Either.Right(key)
        }
    }
}
