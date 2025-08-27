package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.AvailabilityReason
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError as AppContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError as AppPersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError as AppScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError as DomainScopeAliasError
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
 * Maps ScopeAliasError to ApplicationError.ScopeAliasError
 */
fun DomainScopeAliasError.toApplicationError(): ApplicationError = when (this) {
    is DomainScopeAliasError.DuplicateAlias ->
        AppScopeAliasError.AliasDuplicate(
            aliasName = this.aliasName,
            existingScopeId = this.existingScopeId.toString(),
            attemptedScopeId = this.attemptedScopeId.toString(),
        )

    is DomainScopeAliasError.AliasNotFound ->
        AppScopeAliasError.AliasNotFound(
            aliasName = this.aliasName,
        )

    is DomainScopeAliasError.CannotRemoveCanonicalAlias ->
        AppScopeAliasError.CannotRemoveCanonicalAlias(
            scopeId = this.scopeId.toString(),
            aliasName = this.aliasName,
        )

    is DomainScopeAliasError.AliasGenerationFailed ->
        AppScopeAliasError.AliasGenerationFailed(
            scopeId = this.scopeId.toString(),
            retryCount = this.retryCount,
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
    is DomainScopeAliasError -> this.toApplicationError()

    // Map hierarchy errors to appropriate application errors
    is ScopeHierarchyError.HierarchyUnavailable -> {
        val cause = when (this.reason) {
            AvailabilityReason.TEMPORARILY_UNAVAILABLE -> "Hierarchy service temporarily unavailable"
            AvailabilityReason.CORRUPTED_HIERARCHY -> "Hierarchy data corruption detected"
            AvailabilityReason.CONCURRENT_MODIFICATION -> "Concurrent modification conflict"
        }
        AppPersistenceError.StorageUnavailable(
            operation = "hierarchy.${this.operation.name.lowercase()}",
            cause = cause,
        )
    }

    // Map other hierarchy errors to generic persistence errors
    is ScopeHierarchyError -> AppPersistenceError.StorageUnavailable(
        operation = "hierarchy",
        cause = "Hierarchy error: ${this::class.simpleName}",
    )

    // For other errors, create a generic persistence error
    // This should be replaced with context-specific errors in actual handlers
    else -> AppPersistenceError.StorageUnavailable(
        operation = "unknown",
        cause = "Unmapped domain error: ${this::class.simpleName}",
    )
}
