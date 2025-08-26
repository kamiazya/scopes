package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError as AppContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError as AppPersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError as DomainScopeInputError

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
            cause = this.cause?.toString(),
        )

    is DomainPersistenceError.DataCorruption ->
        AppPersistenceError.DataCorruption(
            entityType = this.entityType,
            entityId = this.entityId,
            reason = this.reason,
        )

    is DomainPersistenceError.ConcurrencyConflict ->
        AppPersistenceError.ConcurrencyConflict(
            entityType = this.entityType,
            entityId = this.entityId,
            expectedVersion = this.expectedVersion.toString(),
            actualVersion = this.actualVersion.toString(),
        )

    is DomainPersistenceError.NotFound ->
        AppPersistenceError.NotFound(
            entityType = this.entityType,
            entityId = this.entityId,
        )
}

/**
 * Maps ContextError to ApplicationError.ContextError
 */
fun ContextError.toApplicationError(): ApplicationError = when (this) {
    is ContextError.BlankId ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.attemptedValue,
        )

    is ContextError.InvalidIdFormat ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.attemptedValue,
        )

    is ContextError.EmptyName ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.attemptedValue,
        )

    is ContextError.InvalidNameFormat ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.attemptedValue,
        )

    is ContextError.NameTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.attemptedValue,
        )

    is ContextError.DuplicateName ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.attemptedName,
        )

    is ContextError.ContextNotFound ->
        AppContextError.StateNotFound(
            contextId = this.contextId ?: "unknown",
        )

    is ContextError.InvalidFilter ->
        AppContextError.InvalidFilter(
            filter = this.filter,
            reason = this.reason,
        )

    is ContextError.KeyTooShort ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "key too short",
        )

    is ContextError.KeyTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "key too long",
        )

    is ContextError.InvalidKeyFormat ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = this.reason,
        )

    is ContextError.DescriptionTooShort ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "description too short",
        )

    is ContextError.DescriptionTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "description too long",
        )

    is ContextError.FilterTooShort ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Filter too short",
        )

    is ContextError.FilterTooLong ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Filter too long",
        )

    is ContextError.InvalidFilterSyntax ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = this.reason,
        )

    ContextError.EmptyKey ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty key",
        )

    ContextError.EmptyDescription ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty description",
        )

    ContextError.EmptyFilter ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Empty filter",
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
            expectedFormat = this.expectedFormat,
        )

    is DomainScopeInputError.TitleError.Empty ->
        AppScopeInputError.TitleEmpty(this.attemptedValue)

    is DomainScopeInputError.TitleError.TooShort ->
        AppScopeInputError.TitleTooShort(
            attemptedValue = this.attemptedValue,
            minimumLength = this.minimumLength,
        )

    is DomainScopeInputError.TitleError.TooLong ->
        AppScopeInputError.TitleTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength,
        )

    is DomainScopeInputError.TitleError.ContainsProhibitedCharacters ->
        AppScopeInputError.TitleContainsProhibitedCharacters(
            attemptedValue = this.attemptedValue,
            prohibitedCharacters = this.prohibitedCharacters,
        )

    is DomainScopeInputError.DescriptionError.TooLong ->
        AppScopeInputError.DescriptionTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength,
        )

    is DomainScopeInputError.AliasError.Empty ->
        AppScopeInputError.AliasEmpty(this.attemptedValue)

    is DomainScopeInputError.AliasError.TooShort ->
        AppScopeInputError.AliasTooShort(
            attemptedValue = this.attemptedValue,
            minimumLength = this.minimumLength,
        )

    is DomainScopeInputError.AliasError.TooLong ->
        AppScopeInputError.AliasTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength,
        )

    is DomainScopeInputError.AliasError.InvalidFormat ->
        AppScopeInputError.AliasInvalidFormat(
            attemptedValue = this.attemptedValue,
            expectedPattern = this.expectedPattern,
        )
}

/**
 * Maps ScopeAliasError to ApplicationError
 */
fun ScopeAliasError.toApplicationError(): ApplicationError = when (this) {
    is ScopeAliasError.DuplicateAlias ->
        AppScopeInputError.AliasDuplicate(
            attemptedValue = this.aliasName,
        )

    is ScopeAliasError.AliasNotFound ->
        AppScopeInputError.AliasNotFound(
            attemptedValue = this.aliasName,
        )

    is ScopeAliasError.CannotRemoveCanonicalAlias ->
        AppScopeInputError.CannotRemoveCanonicalAlias(
            attemptedValue = this.aliasName,
        )

    is ScopeAliasError.AliasGenerationFailed ->
        AppPersistenceError.StorageUnavailable(
            operation = "generate alias",
            cause = "Failed to generate alias after ${this.retryCount} attempts",
        )
}

/**
 * Generic fallback for any ScopesError that doesn't have a specific mapping.
 * Use this sparingly - prefer context-specific mappings in handlers.
 */
fun ScopesError.toGenericApplicationError(): ApplicationError = when (this) {
    is DomainPersistenceError -> this.toApplicationError()
    is ContextError -> this.toApplicationError()
    is DomainScopeInputError -> this.toApplicationError()
    is ScopeAliasError -> this.toApplicationError()

    // For other errors, create a generic persistence error
    // This should be replaced with context-specific errors in actual handlers
    else -> AppPersistenceError.StorageUnavailable(
        operation = "unknown",
        cause = "Unmapped domain error: ${this::class.simpleName}",
    )
}

/**
 * Convenience extension for generic error mapping.
 * Delegates to toGenericApplicationError() for consistency.
 */
fun ScopesError.toApplicationError(): ApplicationError = this.toGenericApplicationError()
