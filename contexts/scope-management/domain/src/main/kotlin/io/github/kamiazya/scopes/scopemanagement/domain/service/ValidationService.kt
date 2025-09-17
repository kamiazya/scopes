package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.DomainValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Pagination

/**
 * Domain service for centralized validation logic.
 *
 * This service removes validation responsibility from the interface layer,
 * ensuring consistent validation across all interfaces (MCP, CLI, etc.).
 *
 * Note: ULID validation has been moved to respective value objects (ScopeId, AggregateId).
 * Pagination validation has been moved to the Pagination value object.
 *
 * @param strictMode Whether to use strict validation rules (default: true)
 */
class ValidationService(private val strictMode: Boolean = true) {


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
        val keyRegex = if (strictMode) {
            Regex("^[a-zA-Z0-9-_]{1,64}$") // Strict: alphanumeric with hyphens and underscores
        } else {
            Regex("^[a-zA-Z0-9-_.]{1,64}$") // Lenient: also allow dots
        }

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
