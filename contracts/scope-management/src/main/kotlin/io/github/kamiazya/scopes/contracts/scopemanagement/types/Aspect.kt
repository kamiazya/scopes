package io.github.kamiazya.scopes.contracts.scopemanagement.types

import kotlinx.datetime.Instant

/**
 * Aspect definition data structure.
 */
public data class AspectDefinition(val key: String, val description: String, val type: String, val createdAt: Instant, val updatedAt: Instant)

/**
 * Types of validation failures.
 */
public sealed interface ValidationFailure {
    public data object Empty : ValidationFailure
    public data class TooShort(val minimumLength: Int) : ValidationFailure
    public data class TooLong(val maximumLength: Int) : ValidationFailure
    public data class InvalidFormat(val expectedFormat: String) : ValidationFailure
    public data class InvalidType(val expectedType: String) : ValidationFailure
    public data class NotInAllowedValues(val allowedValues: List<String>) : ValidationFailure
    public data object MultipleValuesNotAllowed : ValidationFailure
}
