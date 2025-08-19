package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.ContextError
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.application.error.ContextError as AppContextError
import io.github.kamiazya.scopes.application.error.PersistenceError as AppPersistenceError
import io.github.kamiazya.scopes.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.domain.error.ScopeInputError as DomainScopeInputError

/**
 * Extension functions for mapping common domain errors to application errors.
 * These provide reusable mappings for errors that don't require special context.
 */

/**
 * Maps PersistenceError to ApplicationError.PersistenceError
 */
fun DomainPersistenceError.toApplicationError(): ApplicationError = when (this) {
    is DomainPersistenceError.StorageUnavailable ->
        AppPersistenceError.StorageUnavailable(
            operation = this.operation,
            cause = this.cause?.toString()
        )

    is DomainPersistenceError.DataCorruption ->
        AppPersistenceError.DataCorruption(
            entityType = this.entityType,
            entityId = this.entityId,
            reason = this.reason
        )

    is DomainPersistenceError.ConcurrencyConflict ->
        AppPersistenceError.ConcurrencyConflict(
            entityType = this.entityType,
            entityId = this.entityId,
            expectedVersion = this.expectedVersion.toString(),
            actualVersion = this.actualVersion.toString()
        )
}

/**
 * Maps ContextError.NamingError to ApplicationError.ContextError
 * Updated to handle separate Key and Name errors
 */
fun ContextError.NamingError.toApplicationError(): ApplicationError = when (this) {
    is ContextError.NamingError.Empty ->
        AppContextError.KeyEmpty // Default to key for backward compatibility

    is ContextError.NamingError.AlreadyExists ->
        AppContextError.KeyAlreadyExists(
            attemptedKey = this.attemptedName
        )

    is ContextError.NamingError.InvalidFormat ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.attemptedName
        )
}

/**
 * Maps ContextError.FilterError to ApplicationError.ContextError
 */
fun ContextError.FilterError.toApplicationError(): ApplicationError = when (this) {
    is ContextError.FilterError.InvalidSyntax ->
        AppContextError.FilterInvalidSyntax(
            position = this.position,
            reason = this.reason,
            expression = this.expression
        )

    is ContextError.FilterError.UnknownAspect ->
        AppContextError.FilterUnknownAspect(
            unknownAspectKey = this.unknownAspectKey,
            expression = this.expression
        )

    is ContextError.FilterError.LogicalInconsistency ->
        AppContextError.FilterLogicalInconsistency(
            reason = this.reason,
            expression = this.expression
        )
}

/**
 * Maps ScopeInputError to ApplicationError.ScopeInputError
 */
fun DomainScopeInputError.toApplicationError(): ApplicationError = when (this) {
    is DomainScopeInputError.IdError.Blank ->
        AppScopeInputError.IdBlank(this.attemptedValue)

    is DomainScopeInputError.IdError.InvalidFormat ->
        AppScopeInputError.IdInvalidFormat(
            attemptedValue = this.attemptedValue,
            expectedFormat = this.expectedFormat
        )

    is DomainScopeInputError.TitleError.Empty ->
        AppScopeInputError.TitleEmpty(this.attemptedValue)

    is DomainScopeInputError.TitleError.TooShort ->
        AppScopeInputError.TitleTooShort(
            attemptedValue = this.attemptedValue,
            minimumLength = this.minimumLength
        )

    is DomainScopeInputError.TitleError.TooLong ->
        AppScopeInputError.TitleTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength
        )

    is DomainScopeInputError.TitleError.ContainsProhibitedCharacters ->
        AppScopeInputError.TitleContainsProhibitedCharacters(
            attemptedValue = this.attemptedValue,
            prohibitedCharacters = this.prohibitedCharacters
        )

    is DomainScopeInputError.DescriptionError.TooLong ->
        AppScopeInputError.DescriptionTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength
        )

    is DomainScopeInputError.AliasError.Empty ->
        AppScopeInputError.AliasEmpty(this.attemptedValue)

    is DomainScopeInputError.AliasError.TooShort ->
        AppScopeInputError.AliasTooShort(
            attemptedValue = this.attemptedValue,
            minimumLength = this.minimumLength
        )

    is DomainScopeInputError.AliasError.TooLong ->
        AppScopeInputError.AliasTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength
        )

    is DomainScopeInputError.AliasError.InvalidFormat ->
        AppScopeInputError.AliasInvalidFormat(
            attemptedValue = this.attemptedValue,
            expectedPattern = this.expectedPattern
        )
}

/**
 * Generic fallback for any ScopesError that doesn't have a specific mapping.
 * Use this sparingly - prefer context-specific mappings in handlers.
 */
fun ScopesError.toGenericApplicationError(): ApplicationError = when (this) {
    is DomainPersistenceError -> this.toApplicationError()
    is ContextError.NamingError -> this.toApplicationError()
    is ContextError.FilterError -> this.toApplicationError()
    is DomainScopeInputError -> this.toApplicationError()

    // For other errors, create a generic persistence error
    // This should be replaced with context-specific errors in actual handlers
    else -> AppPersistenceError.StorageUnavailable(
        operation = "unknown",
        cause = "Unmapped domain error: ${this::class.simpleName}"
    )
}
